"""Builds PDFs from the Markdown documents: Markdown -> HTML (python-markdown) -> PDF (headless Chrome).
Run: analytics/.venv/bin/python docs/report/build_pdf.py
"""
import subprocess
import sys
from pathlib import Path

import markdown
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[2]
CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
CSS = """
@page { size: A4; margin: 16mm 15mm 16mm 15mm; }
body { font-family: -apple-system, 'Segoe UI', Helvetica, Arial, sans-serif; font-size: 9.6pt; line-height: 1.38; color: #0f172a; }
h1 { font-size: 16pt; margin: 0 0 4pt; color: #0f2440; } h2 { font-size: 11.5pt; margin: 11pt 0 3pt; color: #1e3a5f; border-bottom: 1px solid #d9e3f0; padding-bottom: 2pt; }
h3 { font-size: 10pt; margin: 8pt 0 2pt; color: #1e3a5f; } p, li { margin: 2pt 0; } ul, ol { margin: 2pt 0 2pt 16pt; padding: 0; }
table { border-collapse: collapse; width: 100%; margin: 4pt 0 6pt; font-size: 8.4pt; } th, td { border: 1px solid #d9dee7; padding: 2pt 4pt; text-align: left; vertical-align: top; }
th { background: #eef3f9; } code { font-family: Menlo, Consolas, monospace; font-size: 8.2pt; background: #f4f6f9; padding: 0 2px; }
pre { background: #f4f6f9; border: 1px solid #e2e8f0; padding: 5pt 7pt; font-size: 7.9pt; line-height: 1.3; white-space: pre-wrap; margin: 3pt 0 5pt; }
pre code { background: none; padding: 0; } img { max-width: 100%; } .fig { text-align: center; margin: 4pt 0; } .fig p { font-size: 8pt; color: #52514e; }
blockquote { margin: 4pt 0; padding: 3pt 8pt; border-left: 3px solid #2b4c7e; background: #f6f8fb; } .muted { color: #52514e; }
h2, h3 { break-after: avoid; } table, pre, .fig { break-inside: avoid; }
"""

LIST_ITEM = __import__('re').compile(r'^\s*(?:[-*]|\d+\.)\s')


def normalise(text: str) -> str:
    # python-markdown needs a blank line before a list that follows a paragraph (GitHub does not).
    out, in_code = [], False
    for line in text.split('\n'):
        if line.startswith('```'):
            in_code = not in_code
        if (not in_code and LIST_ITEM.match(line) and out and out[-1].strip()
                and not LIST_ITEM.match(out[-1]) and not out[-1].startswith(('|', '<', '    '))):
            out.append('')
        out.append(line)
    return '\n'.join(out)


def build(md_path: Path, pdf_path: Path, title: str):
    body = markdown.markdown(normalise(md_path.read_text(encoding='utf-8')), extensions=['tables', 'fenced_code', 'md_in_html'])
    html = f'<!doctype html><html><head><meta charset="utf-8"><title>{title}</title><style>{CSS}</style></head><body>{body}</body></html>'
    html_path = md_path.with_suffix('.html')
    html_path.write_text(html, encoding='utf-8')
    subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--no-pdf-header-footer', f'--print-to-pdf={pdf_path}',
                    f'--user-data-dir={ROOT / ".chrome-print"}', html_path.as_uri()], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    html_path.unlink()
    pages = len(PdfReader(str(pdf_path)).pages)
    print(f'{pdf_path.relative_to(ROOT)}: {pages} page(s)')
    return pages

if __name__ == '__main__':
    report = build(ROOT / 'docs/report/TECHNICAL_REPORT.md', ROOT / 'docs/report/LeaseLens_Technical_Report.pdf', 'LeaseLens technical report')
    proposal = build(ROOT / 'docs/proposal/PROPOSAL.md', ROOT / 'docs/proposal/LeaseLens_Proposal.pdf', 'LeaseLens proposal')
    if not 5 <= report <= 8 or not 1 <= proposal <= 2:
        sys.exit('Page limits not met: report must be 5-8 pages, proposal 1-2 pages')
