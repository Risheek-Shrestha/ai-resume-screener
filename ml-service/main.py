from fastapi import FastAPI
from pydantic import BaseModel
import pdfplumber
import docx2txt
import io
import re
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sentence_transformers import SentenceTransformer, util

app = FastAPI()

model = SentenceTransformer("all-MiniLM-L6-v2")

SKILL_SYNONYMS = {
    "postgresql": ["postgres", "postgres database"],
    "hibernate": ["jpa", "spring data jpa"],
    "javascript": ["js"],
    "statistics": ["statistical analysis"],
    "kubernetes": ["k8s"],
    "machine learning": ["ml"],
    "deep learning": ["dl"],
    "spring boot": ["spring"],
    "rest api": ["restful api"],
    "scikit-learn": ["sklearn"],
    "tensorflow": ["tf"],
    "pytorch": ["torch"],
    "amazon web services": ["aws"]
}
# ---Helpers----------------------------------------------------------------------------------------------------------

def skill_variants(skill: str) -> list[str]:
    skill = skill.lower()

    variants = [skill]

    if skill in SKILL_SYNONYMS:
        variants.extend(SKILL_SYNONYMS[skill])

    return variants

def skills_match(skill1: str, skill2: str) -> bool:

    s1 = skill1.lower()
    s2 = skill2.lower()

    if s1 == s2:
        return True

    return (
            s2 in skill_variants(s1)
            or
            s1 in skill_variants(s2)
    )


# ── Request / Response models ─────────────────────────────────────────────────

class AnalyzeRequest(BaseModel):
    resumeText: str
    jobSkills: list[str]
    jobDescription: str


class AnalyzeResponse(BaseModel):
    overallScore: float
    matchedKeywords: list[str]
    missingKeywords: list[str]
    recommendationsSummary: str


class SuggestRequest(BaseModel):
    resumeText: str
    jobSkills: list[str]
    jobDescription: str
    jobTitle: str
    overallScore: float
    matchedKeywords: list[str]
    missingKeywords: list[str]


class SuggestResponse(BaseModel):
    scoreLevel: str
    weakAreas: list[str]
    actionableSteps: list[str]
    suggestedLearningPaths: list[str]
    resumeImprovementTips: list[str]


class JobMatchRequest(BaseModel):
    resumeText: str
    resumeSkills: list[str]


class JobMatchCandidate(BaseModel):
    jobId: int
    jobTitle: str
    jobType: str
    experienceLevel: str
    jobSkills: list[str]
    jobDescription: str


class JobMatchBatchRequest(BaseModel):
    resume: JobMatchRequest
    jobs: list[JobMatchCandidate]


class JobMatchResult(BaseModel):
    jobId: int
    jobTitle: str
    jobType: str
    experienceLevel: str
    matchPercentage: float
    matchedSkills: list[str]
    missingSkills: list[str]
    semanticSimilarity: float
    whyMatch: str


# ── Text extraction ───────────────────────────────────────────────────────────

def extract_text(file_bytes: bytes, file_type: str) -> str:
    if "pdf" in file_type:
        with pdfplumber.open(io.BytesIO(file_bytes)) as pdf:
            return " ".join(page.extract_text() or "" for page in pdf.pages)
    elif "word" in file_type or "docx" in file_type or "msword" in file_type:
        return docx2txt.process(io.BytesIO(file_bytes))
    else:
        return file_bytes.decode("utf-8", errors="ignore")


# ── Scoring helpers ───────────────────────────────────────────────────────────

def keyword_score(resume_text: str, skills: list[str]) -> tuple[float, list[str], list[str]]:
    resume_lower = resume_text.lower()
    matched = []
    missing = []
    total_weight = 0
    matched_weight = 0

    for skill in skills:

        variants = skill_variants(skill)
        count = 0

        for variant in variants:
            pattern = r'\b' + re.escape(variant) + r'\b'
            count += len(re.findall(pattern, resume_lower))

        if count == 0:
            weight = 0
            missing.append(skill)
        elif count == 1:
            weight = 0.5
            matched.append(skill)
        elif count == 2:
            weight = 0.75
            matched.append(skill)
        else:
            weight = 1.0
            matched.append(skill)

        total_weight += 1
        matched_weight += weight

    score = (matched_weight / total_weight * 100) if total_weight > 0 else 0

    return round(score, 2), matched, missing


def tfidf_score(resume_text: str, job_description: str) -> float:
    if not resume_text.strip() or not job_description.strip():
        return 0.0
    vectorizer = TfidfVectorizer(stop_words="english")
    try:
        tfidf_matrix = vectorizer.fit_transform([resume_text, job_description])
        similarity = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:2])[0][0]
        return round(float(similarity) * 100, 2)
    except Exception:
        return 0.0


def semantic_score_value(resume_text: str, job_description: str) -> float:
    if not resume_text.strip() or not job_description.strip():
        return 0.0
    resume_excerpt = " ".join(resume_text.split()[:512])
    job_excerpt = " ".join(job_description.split()[:512])
    resume_emb = model.encode(resume_excerpt, convert_to_tensor=True)
    job_emb = model.encode(job_excerpt, convert_to_tensor=True)
    similarity = util.cos_sim(resume_emb, job_emb).item()
    return round(max(0.0, float(similarity)) * 100, 2)


def generate_summary(final_score: float, keyword_pct: float, tfidf_pct: float,
                     semantic_pct: float, missing: list[str]) -> str:
    missing_preview = ", ".join(missing[:5]) if missing else "none"
    if final_score >= 80:
        return (
            f"Strong match ({final_score}%). "
            f"Keyword coverage: {keyword_pct}%, Content similarity: {tfidf_pct}%, "
            f"Semantic alignment: {semantic_pct}%. "
            f"{'Minor gaps in: ' + missing_preview + '.' if missing else 'Excellent skill coverage.'}"
        )
    elif final_score >= 50:

        if missing:
            return (
                f"Moderate match ({final_score}%). "
                f"Keyword coverage: {keyword_pct}%, "
                f"Content similarity: {tfidf_pct}%, "
                f"Semantic alignment: {semantic_pct}%. "
                f"Missing skills: {missing_preview}. "
                f"Tailor your resume and strengthen these areas."
            )

        return (
            f"Moderate match ({final_score}%). "
            f"Keyword coverage: {keyword_pct}%, "
            f"Content similarity: {tfidf_pct}%, "
            f"Semantic alignment: {semantic_pct}%. "
            f"Strong skill coverage with no major gaps detected."
        )
    else:
        return (
            f"Weak match ({final_score}%). "
            f"Keyword coverage: {keyword_pct}%, Content similarity: {tfidf_pct}%, "
            f"Semantic alignment: {semantic_pct}%. "
            f"Key skills to acquire: {missing_preview}. Build experience before applying."
        )


# ── Suggestion helpers ────────────────────────────────────────────────────────

def detect_weak_areas(resume_text: str, missing_skills: list[str],
                      job_description: str, tfidf_pct: float, semantic_pct: float) -> list[str]:
    weak_areas = []
    resume_lower = resume_text.lower()

    # Group missing skills into categories
    categories = {
        "Frontend": ["react", "vue", "angular", "html", "css", "javascript", "typescript", "nextjs"],
        "Backend": ["spring", "django", "node", "express", "fastapi", "flask", "java", "python", "golang"],
        "Database": ["sql", "postgresql", "mysql", "mongodb", "redis", "elasticsearch"],
        "DevOps & Cloud": ["docker", "kubernetes", "aws", "azure", "gcp", "ci/cd", "jenkins", "terraform"],
        "ML & AI": ["machine learning", "deep learning", "tensorflow", "pytorch", "nlp", "scikit-learn"],
        "Testing": ["junit", "pytest", "selenium", "jest", "testing", "tdd"],
        "System Design": ["microservices", "rest api", "graphql", "kafka", "rabbitmq", "system design"],
    }

    missing_lower = [s.lower() for s in missing_skills]
    for category, keywords in categories.items():
        if any(kw in missing_lower for kw in keywords):
            weak_areas.append(f"{category} skills gap")

    # Content depth check using TF-IDF
    if tfidf_pct < 30:
        weak_areas.append("Resume content doesn't closely match the job description language")

    # Semantic gap check
    if semantic_pct < 40:
        weak_areas.append("Overall resume theme doesn't align well with this role")

    # Check for experience indicators
    experience_patterns = [r'\d+\s+years?', r'experience', r'worked on', r'developed', r'built', r'led']
    experience_count = sum(1 for p in experience_patterns if re.search(p, resume_lower))
    if experience_count < 2:
        weak_areas.append("Resume lacks quantifiable experience and impact statements")

    # Check for project mentions
    if 'project' not in resume_lower and 'github' not in resume_lower:
        weak_areas.append("No projects or portfolio mentioned — hard to assess practical skills")

    return weak_areas if weak_areas else ["No major weak areas detected — focus on polishing"]


def generate_actionable_steps(score: float, missing_skills: list[str],
                              weak_areas: list[str], job_title: str) -> list[str]:
    steps = []
    missing_preview = ", ".join(missing_skills[:4]) if missing_skills else None

    if score < 50:
        steps.append(f"This role requires significant upskilling — consider a structured learning plan")
        if missing_preview:
            steps.append(f"Priority skills to learn: {missing_preview}")
        steps.append("Look for junior or internship roles in this domain to build experience first")
        steps.append("Build 2-3 portfolio projects demonstrating the required tech stack")
    elif score < 80:
        if missing_preview:
            steps.append(f"Add hands-on projects using: {missing_preview}")
            steps.append(f"Explicitly list {missing_preview} in your skills section if you have any exposure")
        steps.append("Rewrite your resume summary to directly reference this job's requirements")
        steps.append("Quantify your achievements — add numbers, scale, and business impact")
    else:
        steps.append("Your profile is strong — focus on interview preparation")
        steps.append("Prepare examples of past work that directly match this role")
        steps.append("Research the company and tailor your cover letter specifically")

    if "No projects or portfolio mentioned" in str(weak_areas):
        steps.append("Create a GitHub profile and push your projects — it's crucial for tech roles")

    if "lacks quantifiable experience" in str(weak_areas):
        steps.append("Rewrite bullet points with impact: 'Reduced API latency by 40%' not 'Worked on APIs'")

    return steps


def generate_learning_paths(missing_skills: list[str]) -> list[str]:
    """Map missing skills to free learning resources."""
    resource_map = {
        "react": "React — official docs (react.dev) + freeCodeCamp React course",
        "spring": "Spring Boot — spring.io guides + Amigoscode YouTube",
        "docker": "Docker — Play with Docker (labs.play-with-docker.com) + TechWorld with Nana YouTube",
        "kubernetes": "Kubernetes — killer.sh free tier + KodeKloud free intro",
        "aws": "AWS — AWS Free Tier + AWS Skill Builder free courses",
        "python": "Python — Python.org tutorial + CS50P (free on edX)",
        "machine learning": "ML — Andrew Ng's ML Specialization (audit free on Coursera)",
        "postgresql": "PostgreSQL — PostgreSQL Tutorial (postgresqltutorial.com)",
        "typescript": "TypeScript — typescriptlang.org handbook (free)",
        "kafka": "Kafka — Apache Kafka quickstart + Confluent free courses",
        "kubernetes": "Kubernetes — Kubernetes.io tutorials + Kelsey Hightower's guide",
        "tensorflow": "TensorFlow — tensorflow.org tutorials + DeepLearning.AI free intro",
        "graphql": "GraphQL — graphql.org learn section + Apollo free tutorials",
    }

    paths = []
    for skill in missing_skills[:5]:
        skill_lower = skill.lower()
        for key, resource in resource_map.items():
            if key in skill_lower or skill_lower in key:
                paths.append(resource)
                break
        else:
            paths.append(f"{skill} — search '{skill} tutorial' on YouTube or freeCodeCamp.org")

    return paths if paths else ["Explore freeCodeCamp.org and The Odin Project for free structured learning"]


def generate_resume_tips(resume_text: str, tfidf_pct: float,
                         semantic_pct: float, job_description: str) -> list[str]:
    """Generate resume writing tips based on content analysis."""
    tips = []
    resume_lower = resume_text.lower()

    # Check length
    word_count = len(resume_text.split())
    if word_count < 200:
        tips.append("Your resume seems too short — add more detail about your experience and projects")
    elif word_count > 1000:
        tips.append("Your resume may be too long — keep it to 1-2 pages, focus on relevant experience")

    # Check for action verbs
    action_verbs = ["built", "developed", "designed", "implemented", "led", "improved",
                    "reduced", "increased", "architected", "deployed", "optimized"]
    verb_count = sum(1 for v in action_verbs if v in resume_lower)
    if verb_count < 3:
        tips.append("Use strong action verbs: Built, Developed, Architected, Optimized, Led, Reduced")

    # Check for numbers/metrics
    if not re.search(r'\d+%|\d+ years?|\d+x|\$\d+', resume_lower):
        tips.append("Add measurable impact — percentages, scale, users, revenue (e.g. 'Served 10k users')")

    # Content similarity gap
    if tfidf_pct < 40:
        tips.append("Mirror the job description language in your resume — use the same terminology")

    # Semantic gap
    if semantic_pct < 50:
        tips.append("Your resume reads as a different domain — reframe your experience toward this role")

    # Check for education/certifications
    if "certification" not in resume_lower and "certified" not in resume_lower:
        tips.append("Consider adding relevant certifications — they signal commitment to the field")

    return tips if tips else ["Your resume structure looks good — focus on tailoring content per application"]


# ── Endpoints ─────────────────────────────────────────────────────────────────

@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(request: AnalyzeRequest):
    resume_text = request.resumeText

    kw_score, matched, missing = keyword_score(resume_text, request.jobSkills)
    tf_score = tfidf_score(resume_text, request.jobDescription)
    sem_score = semantic_score_value(resume_text, request.jobDescription)

    final_score = round((kw_score * 0.4) + (tf_score * 0.3) + (sem_score * 0.3), 2)
    summary = generate_summary(final_score, kw_score, tf_score, sem_score, missing)

    return AnalyzeResponse(
        overallScore=final_score,
        matchedKeywords=matched,
        missingKeywords=missing,
        recommendationsSummary=summary
    )


class ExtractTextRequest(BaseModel):
    fileData: list[int]
    fileType: str


@app.post("/extract-text")
def extract_resume_text(request: ExtractTextRequest):
    file_bytes = bytes(request.fileData)

    text = extract_text(
        file_bytes,
        request.fileType
    )

    return {
        "parsedText": text
    }


@app.post("/suggest", response_model=SuggestResponse)
def suggest(request: SuggestRequest):
    resume_text = request.resumeText

    # Re-compute content-based scores for deeper analysis
    tf_score = tfidf_score(resume_text, request.jobDescription)
    sem_score = semantic_score_value(resume_text, request.jobDescription)

    # Score level
    if request.overallScore >= 80:
        score_level = "STRONG"
    elif request.overallScore >= 50:
        score_level = "MODERATE"
    else:
        score_level = "WEAK"

    weak_areas = detect_weak_areas(
        resume_text, request.missingKeywords, request.jobDescription, tf_score, sem_score
    )
    actionable_steps = generate_actionable_steps(
        request.overallScore, request.missingKeywords, weak_areas, request.jobTitle
    )
    learning_paths = generate_learning_paths(request.missingKeywords)
    resume_tips = generate_resume_tips(resume_text, tf_score, sem_score, request.jobDescription)

    return SuggestResponse(
        scoreLevel=score_level,
        weakAreas=weak_areas,
        actionableSteps=actionable_steps,
        suggestedLearningPaths=learning_paths,
        resumeImprovementTips=resume_tips
    )


@app.post("/match-jobs", response_model=list[JobMatchResult])
def match_jobs(request: JobMatchBatchRequest):
    resume_text = request.resume.resumeText
    resume_skills = request.resume.resumeSkills

    results = []

    for job in request.jobs:

        required = job.jobSkills

        matched = [
            s for s in resume_skills
            if any(skills_match(s, r) for r in required)
        ]

        missing = [
            r for r in required
            if not any(skills_match(r, s) for s in resume_skills)
        ]

        kw_pct = round(
            (len(matched) / len(required) * 100),
            2
        ) if required else 0

        sem_pct = semantic_score_value(
            resume_text,
            job.jobDescription
        )

        tf_pct = tfidf_score(
            resume_text,
            job.jobDescription
        )

        match_pct = round(
            (kw_pct * 0.4)
            + (tf_pct * 0.3)
            + (sem_pct * 0.3),
            2
        )

        if match_pct >= 70:
            why = (
                f"Strong fit — your skills and experience closely align "
                f"with this {job.jobTitle} role."
            )
        elif match_pct >= 40:
            why = (
                f"Partial fit — you match {len(matched)} of "
                f"{len(required)} required skills. "
                f"{'Gap areas: ' + ', '.join(missing[:3]) if missing else ''}"
            )
        else:
            why = (
                f"Low fit — significant skill gap for this role. "
                f"Missing: {', '.join(missing[:3]) if missing else 'core required skills'}."
            )

        results.append(
            JobMatchResult(
                jobId=job.jobId,
                jobTitle=job.jobTitle,
                jobType=job.jobType,
                experienceLevel=job.experienceLevel,
                matchPercentage=match_pct,
                matchedSkills=matched,
                missingSkills=missing,
                semanticSimilarity=sem_pct,
                whyMatch=why
            )
        )

    results.sort(
        key=lambda x: x.matchPercentage,
        reverse=True
    )

    return results[:5]
