#!/bin/bash

hbase_truncate.sh cg-match-eki
job_match_and_analyze.sh cg match-eki cg-match-eki

hbase_truncate.sh cg-match-eki2
job_match_and_analyze.sh cg match-eki2 cg-match-eki2

hbase_truncate.sh cg-match-isbn
job_match_and_analyze.sh cg match-isbn cg-match-isbn

hbase_truncate.sh cg-match-oclc
job_match_and_analyze.sh cg match-oclc cg-match-oclc

hbase_truncate.sh cg-match-title
job_match_and_analyze.sh cg match-title cg-match-title

