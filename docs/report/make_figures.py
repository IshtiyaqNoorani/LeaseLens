"""Report figures drawn directly from the committed benchmark CSVs (no numbers typed by hand).
Run: analytics/.venv/bin/python docs/report/make_figures.py
"""
from pathlib import Path
import pandas as pd
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'benchmarks' / 'results'
OUT = Path(__file__).with_name('figures')
OUT.mkdir(exist_ok=True)
BLUE, ORANGE, AQUA = '#2a78d6', '#eb6834', '#1baf7a'
plt.rcParams.update({'font.size': 9, 'axes.spines.top': False, 'axes.spines.right': False, 'axes.grid': True,
                     'grid.color': '#e1e0d9', 'axes.axisbelow': True})

def load(name):
    return pd.read_csv(RES / f'{name}.csv', comment='#')

st = load('segment_tree')
fig, axes = plt.subplots(1, 2, figsize=(7.2, 2.6))
for ax, (ratio, label) in zip(axes, [('update_ratio=0.00', 'queries only'), ('update_ratio=0.10', '10% corrections')]):
    w = st[(st.experiment == 'workload') & (st.parameter == ratio) & (st.workload.str.contains('random'))]
    for alg, c, name in [('SegmentTree', BLUE, 'Segment Tree'), ('LinearScan', ORANGE, 'Linear scan'),
                         ('PrefixSums', AQUA, 'Prefix sums (sum only)')]:
        d = w[w.algorithm == alg].sort_values('n')
        ax.plot(d.n, d.median_ns / 1e6, marker='o', color=c, label=name, linewidth=1.8, markersize=4)
    ax.set_xscale('log'); ax.set_yscale('log'); ax.set_xlabel('n (days)'); ax.set_title(f'10,000 operations, {label}')
axes[0].set_ylabel('median time (ms)'); axes[0].legend(fontsize=7, frameon=False)
fig.tight_layout(); fig.savefig(OUT / 'bench_segment_tree.png', dpi=200)

qs = load('quickselect')
fig, axes = plt.subplots(1, 2, figsize=(7.2, 2.6))
s = qs[(qs.experiment == 'single-percentile') & (qs.workload == 'uniform')]
for alg, c, name in [('RandomizedQuickselect', BLUE, 'Randomized Quickselect'), ('Arrays.sort', ORANGE, 'Arrays.sort + index')]:
    d = s[s.algorithm == alg].sort_values('n')
    axes[0].plot(d.n, d.median_ns / 1e3, marker='o', color=c, label=name, linewidth=1.8, markersize=4)
axes[0].set_xscale('log'); axes[0].set_yscale('log'); axes[0].set_xlabel('n'); axes[0].set_ylabel('median time (µs)')
axes[0].set_title('One percentile (P90), uniform data'); axes[0].legend(fontsize=7, frameon=False)
k = qs[(qs.experiment == 'k-percentiles') & (qs.n == 100000)]
for shape, ls in [('uniform', '-'), ('delays', '--')]:
    q = k[(k.algorithm == 'RandomizedQuickselect') & (k.workload == shape)].copy()
    srt = k[(k.algorithm == 'Arrays.sort') & (k.workload == shape)].copy()
    q['k'] = q.parameter.str[2:].astype(int); srt['k'] = srt.parameter.str[2:].astype(int)
    m = q.merge(srt, on='k', suffixes=('_q', '_s')).sort_values('k')
    axes[1].plot(m.k, m.median_ns_s / m.median_ns_q, ls, marker='o', color=BLUE if shape == 'uniform' else ORANGE,
                 label=f'{shape} data', linewidth=1.8, markersize=4)
axes[1].axhline(1, color='#52514e', linewidth=1)
axes[1].set_xlabel('number of percentiles k'); axes[1].set_ylabel('sort time ÷ quickselect time')
axes[1].set_title('n = 100,000: above 1, quickselect is faster'); axes[1].legend(fontsize=7, frameon=False)
fig.tight_layout(); fig.savefig(OUT / 'bench_quickselect.png', dpi=200)
print('figures written to', OUT)
