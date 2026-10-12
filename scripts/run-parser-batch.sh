#!/usr/bin/env bash
set -uo pipefail
phase="${1:?phase required}"
enforce="${2:?true or false required}"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.creditrepairai.v2.analysis.SyntheticParserBatchTest \
  -Pandroid.testInstrumentationRunnerArguments.parserPhase="$phase" \
  -Pandroid.testInstrumentationRunnerArguments.parserEnforce="$enforce" --stacktrace
result=$?
adb exec-out run-as com.creditrepairai.v2.debug cat "files/parser-validation-$phase.json" > "parser-validation-$phase.json"
if ! test -s "parser-validation-$phase.json"; then exit 1; fi
exit "$result"
