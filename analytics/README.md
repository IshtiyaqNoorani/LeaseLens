# LeaseLens exploratory data analysis (supplementary)

`LeaseLens_EDA.ipynb` explores the synthetic sample portfolio with pandas, NumPy, SciPy and Matplotlib. The notebook is committed **with its outputs**, so it can be read without running anything. Figures are also saved in `figures/` for the report.

It does four things:
1. Re-checks 18 figures from the Java application, recomputed independently in pandas (all match exactly).
2. Covers descriptive statistics; rent, delay, occupancy and expense distributions; segment comparisons; trends and seasonality.
3. Runs a Kaplan–Meier estimate of time to payment that accounts for obligations still open (right-censoring).
4. Tests whether the analysis recovers the generator's planted ground truth: 5 of 6 recovered, 1 partly.

## Run it
```bash
python3 -m venv analytics/.venv          # Python 3.11+ (developed with 3.13)
analytics/.venv/bin/pip install -r analytics/requirements.txt
analytics/.venv/bin/python analytics/build_notebook.py
cd analytics && .venv/bin/jupyter nbconvert --to notebook --execute --inplace LeaseLens_EDA.ipynb
```
`build_notebook.py` is the source of the notebook's cells; edit that file rather than the `.ipynb`. `api_snapshot.json` was captured from the running Java API on the sample dataset and is used only for the cross-check.

All findings describe a **synthetic** portfolio. Correlations are not causal, and nothing here judges any tenant.
