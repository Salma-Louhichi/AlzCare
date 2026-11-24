# =============================================================================
# AlzCare ML prediction service (FastAPI + scikit-learn)
# The model is trained during the build from the seeded synthetic generator, so the
# image is reproducible and no binary model file is stored in git.
# =============================================================================
FROM python:3.12-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY app ./app
COPY training ./training

# Train and evaluate the model; fails the build if training breaks
RUN python -m training.train --out app/artifacts

RUN groupadd --system alzcare && useradd --system --gid alzcare --no-create-home alzcare \
    && chown -R alzcare:alzcare /app
USER alzcare

EXPOSE 8094

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://localhost:8094/health').status == 200 else 1)"

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8094", "--proxy-headers"]
