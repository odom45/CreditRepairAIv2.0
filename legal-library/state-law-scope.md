# State-law knowledge scope

Reviewed: 2026-09-16

CreditRepairAI maintains a jurisdiction registry for all 50 states and the District of Columbia. A registry entry is not itself a statement of law. It identifies an official source portal and the categories that must be reviewed before the agent may make a state-specific claim.

## Required state source categories

1. Credit-services or credit-repair organization requirements: registration, bonding, written contracts, disclosures, cancellation periods, fee timing, prohibited practices, and advertising restrictions.
2. State consumer-reporting requirements: accuracy, dispute/reinvestigation, file disclosure, security freezes, identity-theft blocking, permissible purpose, employment/tenant reports, and medical-debt provisions.
3. Furnisher and debt-collection requirements that directly affect reported tradelines, validation, re-aging, or dispute handling.
4. State unfair or deceptive acts and practices statutes and official attorney-general guidance relevant to credit-repair services.
5. Licensing and regulator guidance from the state banking, financial-services, consumer-credit, or business-registration authority.
6. State privacy, breach-notification, deletion, biometric, and sensitive-data requirements applicable to report-derived information.
7. Effective dates, amendments, repeals, agency rules, official forms, and controlling official guidance.

## Coverage states

- `SOURCE_REGISTRY_ONLY`: The official code portal is registered, but no provision has been approved for state-specific reasoning. The agent must use federal sources and tell the user that state coverage is pending.
- `SOURCE_TEXT_VERIFIED`: Current official text, citation, effective date, and source URL have been checked. The agent may accurately summarize the text but must not invent an interpretation.
- `REVIEW_DUE`: Previously verified material has passed its review deadline or a source change was detected. State-specific use is disabled until reverified.
- `RETIRED`: The provision was repealed, superseded, or moved. It remains in history but is not used for current guidance.

## Deliberately deferred

Attorney-reviewed interpretations, litigation strategy, private-right-of-action analysis, damages, limitation periods, preemption analysis, and escalation rules are outside this phase. Those questions must be identified as requiring qualified counsel in the relevant jurisdiction.
