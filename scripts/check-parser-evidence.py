"""Fail CI when active follow-up docs disagree with the versioned corpus/evidence."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
evidence = json.loads((root / 'docs/PARSER_ROBUSTNESS_EVIDENCE.json').read_text())
status = evidence['status']
assert status in {'implemented-awaiting-ci', 'verified'}, 'Unknown evidence status'
marker = f'<!-- parser-followup: {status} -->'
for name in ['docs/PRODUCTION_ASSESSMENT.md', 'RELEASE_OWNER_CHECKLIST.md']:
    text = (root / name).read_text()
    assert text.count(marker) == 1, f'{name}: mismatched status'
    if 'codeReadinessPercent' in evidence:
        assert f"Code readiness: **{evidence['codeReadinessPercent']}%**" in text, f'{name}: mismatched code rating'
    assert len(re.findall(r'<!-- parser-followup:', text)) == 1
    assert not re.search(r'\b(?:55|65)/100\b', text), 'Historical score in current assessment'
    if status == 'verified':
        assert evidence['verificationRun'] and evidence['verificationRun'] in text
        assert 'CI verification pending' not in text
    else:
        assert evidence['verificationRun'] is None
corpus = root / 'native-android/app/src/test/resources/parser/corpus'
rows = [line.split('\t') for line in (corpus / 'manifest.tsv').read_text().splitlines() if line and not line.startswith('#')]
assert len(rows) == evidence['checkedInTextFixtures'] == 18
assert len({row[0] for row in rows}) == len(rows)
for bureau in ['EQUIFAX', 'EXPERIAN', 'TRANSUNION']:
    assert sum(row[1] == bureau for row in rows) == evidence['textLayoutsPerBureau'] == 6
assert {p.name for p in corpus.glob('*.txt')} == {row[0] for row in rows}
batch = (root / 'native-android/app/src/androidTest/java/com/creditrepairai/v2/analysis/SyntheticParserBatchTest.kt').read_text()
layouts = re.search(r'private val layouts = listOf\(([^\n]+)\)', batch).group(1)
scans = re.search(r'private val scans = listOf\(([^\n]+)\)', batch).group(1)
assert 3 * (len(re.findall(r'"[^"]+"', layouts)) + len(re.findall(r'"[^"]+"', scans)) + 2) == evidence['supportedPdfCases'] == 72
assert evidence['rejectionPdfCases'] == 6
assert evidence['totalPdfCases'] == evidence['supportedPdfCases'] + evidence['rejectionPdfCases'] == 78
print(f'Parser follow-up evidence and both active checklists agree: {status}; 18 text / 78 PDF cases')
