# Google Play closed testing and production-access record

Status on 2026-10-12: **NOT STARTED / CONSOLE ACCESS UNAVAILABLE**. No tester has been enrolled by this task, no 14-day clock is claimed to have started, and no production-access approval has been obtained. The signed AAB exists; that is packaging evidence only.

Official policy: https://support.google.com/googleplay/android-developer/answer/14151465

The specific 12-testers/14-continuous-days requirement applies to personal developer accounts created after November 13, 2023. Confirm the app's actual Play Console requirement and account type. If applicable, at least 12 testers must be continuously opted in for the preceding 14 days when applying for production access. Meeting that minimum enables an application; it is not automatic approval. A production draft does not imply an app or production-access review has passed.

## Owner inputs needed to start

- Working access to the existing Play app/listing, and permission to upload/configure its closed track; no Google sign-in credentials in chat.
- 12 real willing Android testers with Google accounts; recruit 16 if possible to provide a voluntary participation buffer. The roster is private and must not be committed in Git or publicly posted. No automated fake accounts or invented opt-in evidence.
- The app's Play App Signing certificate, genuine Firebase registrations/configs, a passing native Google/email sign-in walkthrough, support contact, privacy/deletion URLs and accurate reviewer App access instructions.
- Funded optional AI if included in the tested flow. Paid access currently defaults off; do not charge testers. Play billing tests use authorized license-test accounts, not real charges.

## Create a closed release

1. Confirm package `com.creditrepairai.v2`, existing upload certificate and version code above every previous upload. Upload the verified `.aab` to the intended closed testing track; keep public production rollout unpublished.
2. Complete App content, required policies, Data Safety, target audience, store listing, countries and App access answers based on actual tested behavior. Do not assert account deletion, privacy or AI behavior that was not verified.
3. Select an email list or owned Google Group for the real tester roster. Add only authorized participants and copy **the actual Console-generated opt-in URL** here: **[pending Console access]**. Sending recruitment/invitations is a separate explicit user-directed action; none were sent by this task.
4. Submit the closed-track release for the required review and resolve Console validation issues. A draft upload alone does not start testing.
5. Have each tester accept the opt-in, install through Play and confirm that app login works. Sideloaded APK downloads do not prove Play opt-in. Record opt-in timestamps separately from installation timestamps.
6. Record the earliest eligibility date only after at least 12 participants have confirmed opt-in. Each qualifying participant must individually satisfy continuity; dropping below a qualifying cohort can move the eligible date. Use Console evidence as authoritative.

## Tester script using fictional data

| Window | Activities | Evidence |
| --- | --- | --- |
| Day 1 | Play install; Google/email sign-in; policies/onboarding; import a synthetic report | Build/version, Android/device, opt-in and login result, reproducible issue details. |
| Days 2–4 | Three-bureau imports; verify account counts, suffixes, balances, missing score; scanned PDF import | Field comparison and extraction method; no real consumer report uploads. |
| Days 5–7 | Review findings, prepare draft, copy a section, save a fictional filing receipt/response | Navigation/reachability and user-review behavior; do not file a real dispute as a test. |
| Days 8–10 | Funded optional AI with fictional cases and retrieved sources; AI-consent revoke; font-size/TalkBack checks | Expected rejection after withdrawal, useful source citations, accessibility issues. |
| Days 11–13 | Sign out/back in; account isolation; interruption/relaunch; disposable-account deletion/re-authentication | No old case crosses accounts; completion/error states recorded. |
| Day 14+ | Check qualifying opt-in continuity, resolve blockers, summarize real feedback and fixes | Console evidence; named issue/commit references; confirmed qualifying tester count. |

The daily usage schedule is a test plan for useful feedback; Google separately specifies continuous opt-in and asks about tester engagement. Never fill a log from the planned schedule as if those actions happened.

## Private roster template

Copy this blank template into a private owner-controlled record. Populate real details only after participant consent; do not push it to Git. Tester IDs are labels, not people or enrollments.

| Tester ID | Private Google-account reference | Opted in at | Installed/build | Last feedback | Still opted in | Qualifying 14 days |
| --- | --- | --- | --- | --- | --- | --- |
| T01 | pending | pending | pending | pending | unverified | no |
| T02 | pending | pending | pending | pending | unverified | no |
| T03 | pending | pending | pending | pending | unverified | no |
| T04 | pending | pending | pending | pending | unverified | no |
| T05 | pending | pending | pending | pending | unverified | no |
| T06 | pending | pending | pending | pending | unverified | no |
| T07 | pending | pending | pending | pending | unverified | no |
| T08 | pending | pending | pending | pending | unverified | no |
| T09 | pending | pending | pending | pending | unverified | no |
| T10 | pending | pending | pending | pending | unverified | no |
| T11 | pending | pending | pending | pending | unverified | no |
| T12 | pending | pending | pending | pending | unverified | no |
| T13–T16 optional | pending | pending | pending | pending | unverified | no |

## Production-access application worksheet — complete from evidence later

- Number of qualifying opted-in testers and uninterrupted date range: **pending**.
- How testers were recruited, actual usage/engagement, and how feedback was collected: **pending real evidence**.
- Concrete problems identified, fixes, commits/builds and retest outcomes: **pending real evidence**.
- Intended audience/value and what changed to make the app ready: **pending completed testing**.
- Console production-access decision and date: **pending**.
- Public production rollout: **remain unpublished unless owner requests launch**.

Transfer the final private testing/approval record to the buyer through the agreed secure hand-off. This plan and blank template do not establish testing completion, compliance, launch approval or valuation.
