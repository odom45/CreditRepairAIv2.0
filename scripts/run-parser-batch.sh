#!/usr/bin/env bash
set -uo pipefail
phase="${1:?phase required}"
enforce="${2:?true or false required}"
adb logcat -c
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.creditrepairai.v2.analysis.SyntheticParserBatchTest,com.creditrepairai.v2.analysis.PdfParserInstrumentedTest,com.creditrepairai.v2.analysis.PdfParserFuzzTest \
  -Pandroid.testInstrumentationRunnerArguments.parserPhase="$phase" \
  -Pandroid.testInstrumentationRunnerArguments.parserEnforce="$enforce" --stacktrace
result=$?
adb logcat -d -v raw -s ParserBatchChunk:I '*:S' > parser-validation-chunks.txt
python3 - "$phase" <<'PY'
import json,re,sys
from pathlib import Path
chunks={}
for line in Path('parser-validation-chunks.txt').read_text().splitlines():
    match=re.match(r'^(\d{5}):(.*)$',line)
    if match: chunks[int(match[1])]=match[2]
assert chunks and sorted(chunks)==list(range(len(chunks))), 'Missing metric log chunks'
data=json.loads(''.join(chunks[i] for i in range(len(chunks))))
assert data['total']==78 and data['supportedCases']==72 and len(data['cases'])==78
assert data['phase']==sys.argv[1]
Path('parser-validation-'+sys.argv[1]+'.json').write_text(json.dumps(data,indent=2)+'\n')
print({k:v for k,v in data.items() if k!='cases'})
PY
if [ $? -ne 0 ]; then exit 1; fi
exit "$result"
