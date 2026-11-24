# Model card — care-risk-lr-1.0.0

> **Demonstration model trained on synthetic data. Not clinically validated.**
> It must not be used for diagnosis or treatment decisions.

## Purpose
Help a doctor notice which patients may need closer follow-up: the model estimates the
probability that a patient's **next autonomy assessment shows a decline of at least 2 points**
on AlzCare's 1–10 autonomy scale.

## Inputs
Derived from the patient's two most recent autonomy assessments (identity-service) and the
date of birth:

| Feature | Meaning |
|---|---|
| `age_years` | Age at the latest assessment |
| `hygiene`, `medication`, `mobility`, `feeding` | Level in each care area: 3 = INDEPENDENT, 2 = ASSISTED, 1 = DEPENDENT |
| `score_change` | Overall score of the latest minus the previous assessment |
| `months_since_previous` | Time between the two assessments |
| `has_previous` | Whether a previous assessment exists (trend features are 0 otherwise) |

The overall score is **not** an input: it is computed from the four levels, and using both made
the per-feature explanations contradictory (collinearity).

## Model
`StandardScaler` + `LogisticRegression` (scikit-learn). A histogram gradient-boosting classifier
is trained as a baseline on the same split. The logistic regression is deployed because each
prediction can be explained: every feature contributes `coefficient × standardised value` to the
log-odds, and the service returns the three largest contributions as plain-language factors.

Risk levels: LOW < 0.30 ≤ MODERATE < 0.60 ≤ HIGH.

## Training data
**Synthetic.** 6,000 patients produced by `training/synthetic.py` (seed 42) from an explicit
generative process: a hidden progression value drives age, the loss of autonomy in each area
(medication first, feeding last) and the speed of decline; the label's log-odds rise with age,
recent decline, medication dependence and the hidden progression, plus noise. 80/20 stratified
split; 42 % positives.

## Evaluation (hold-out, 1,200 synthetic patients)

| Model | ROC-AUC | Brier | Accuracy @ 0.5 |
|---|---|---|---|
| Logistic regression (deployed) | 0.787 | 0.184 | 0.733 |
| Gradient boosting (baseline) | 0.774 | 0.191 | 0.711 |

Because part of the label depends on a hidden variable, the achievable AUC is limited; the
numbers show the pipeline works, not that the model would generalise to real patients.

## Limitations
- The model can only reproduce the assumptions encoded in the generator. Real-world
  performance is unknown.
- Only autonomy levels, trend and age are used. Diagnosis stage, comorbidities, medication
  and behaviour logs are not.
- No fairness analysis was possible (no demographic data beyond age).

## What real use would require
A labelled, consented clinical dataset; external validation; calibration checks per subgroup;
monitoring for drift; and clinical governance before the output is shown to care teams.

## Reproducing
```bash
pip install -r requirements-dev.txt
python -m training.train          # writes app/artifacts/model.joblib + metadata.json
pytest                            # 35 tests
```
