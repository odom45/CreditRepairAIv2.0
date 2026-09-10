package com.creditrepairai.v2.ai

import com.creditrepairai.v2.analysis.CreditAnalyzer
import com.creditrepairai.v2.model.AppState
import java.util.Locale

object CreditAssistant {
    fun respond(prompt: String, state: AppState): String {
        val input = prompt.lowercase(Locale.US)
        val highest = state.findings.firstOrNull()
        return when {
            state.reports.isEmpty() && !input.contains("demo") ->
                "Start by importing original PDF reports from TransUnion, Experian, and Equifax. I analyze data stored on this device and flag differences for your review. You can also load the safe demo to test every screen."

            listOf("first", "start", "priority", "next").any(input::contains) ->
                highest?.let {
                    "Your first review item is ${it.creditor}: ${it.title.lowercase()}. ${it.detail} Confirm the facts against statements before generating or sending a dispute."
                } ?: "I do not see a cross-bureau inconsistency yet. Import all available reports, confirm balances and dates, then focus on on-time payments and revolving utilization."

            listOf("score", "fast", "immediate", "how long", "timeline").any(input::contains) ->
                CreditAnalyzer.impactPlan(state.findings, state.disputes).joinToString("\n\n") {
                    "${it.window} — ${it.title}: ${it.expectation}"
                } + "\n\nNo app can guarantee points or a particular timing because lenders and scoring models differ."

            listOf("cfpb", "complaint").any(input::contains) ->
                "Use a CFPB complaint after you can state the exact reporting problem and requested resolution. The app prepares a reviewable narrative and opens the official portal; you remain in control of attachments and submission."

            listOf("dispute", "letter", "law", "fcra").any(input::contains) ->
                if (state.findings.isEmpty()) {
                    "A dispute should identify a specific inaccurate or incomplete fact. Import reports first; I will map supported findings to relevant FCRA provisions such as 15 U.S.C. §§ 1681e(b) and 1681i."
                } else {
                    "There are ${state.findings.size} review finding(s) and ${state.disputes.size} saved dispute(s). Generate a letter from a finding, replace all placeholders, attach copies of evidence, and keep proof of delivery. Never dispute accurate information merely because it is negative."
                }

            listOf("freeze", "secondary", "lexis", "innovis").any(input::contains) ->
                "A security freeze can limit new access to a consumer-reporting file, but it does not remove accurate negative information and may affect applications. Use each bureau’s official site and record the PIN or confirmation securely."

            else -> buildString {
                append("I found ${state.reports.size} imported report(s), ${state.accounts.size} parsed account entries, ${state.findings.size} review finding(s), and ${state.disputes.size} saved dispute(s). ")
                if (highest != null) append("The highest-priority item is ${highest.creditor}: ${highest.title}. ")
                append("Ask me ‘what should I do first,’ ‘explain my score timeline,’ ‘help with CFPB,’ or ‘explain my disputes.’")
            }
        }
    }
}
