# CreditRepairAI Viktor Space Migration Todo

## Backend (Convex)
- [ ] Update `convex/schema.ts` with `reports` and `disputes` tables.
- [ ] Create `convex/reports.ts` for report parsing and storage.
    - [ ] Port `CreditReportParser.js` logic to a Convex action (using `pdf-parse` if possible, or SDK tool).
    - [ ] Implement `compareAccounts` logic.
- [ ] Create `convex/disputes.ts` for dispute CRUD.
- [ ] Update `convex/viktorTools.ts` if needed for AI analysis.

## Frontend (React + shadcn/ui)
- [ ] Create `src/pages/UploadPage.tsx` (Port from `UploadScreen.js`).
- [ ] Create `src/pages/AnalysisPage.tsx` (Port from `AnalysisScreen.js`).
- [ ] Create `src/pages/DisputesPage.tsx` (Port from `DisputesScreen.js`).
- [ ] Update `src/App.tsx` with new routes.
- [ ] Update `src/components/AppSidebar.tsx` with new navigation items.
- [ ] Update `src/pages/LandingPage.tsx` with CreditRepairAI branding.

## Testing & Deployment
- [ ] Run `bun run sync:build` to verify build.
- [ ] Write Playwright e2e tests for upload and analysis.
- [ ] Deploy to preview and share with user.
- [ ] Deploy to production after approval.
