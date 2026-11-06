package com.a9.arbor.grc.modules.privacy.service;

import java.util.List;

/**
 * PIMS DPIA bank (from DPIA CCTV sample): screening Q4–7 gate, 17 questions, 11 assessment principles.
 */
public final class DpiaMethodologyBank {

    public record Question(int number, String principle, String question) {
    }

    public record Principle(int number, String name, String description) {
    }

    public static final List<String> SCREENING_Q4_TO_Q7 = List.of(
            "Will the processing involve a large volume of Sensitive Personal Data?",
            "Will the processing involve a systematic and comprehensive assessment of the personal aspects of the Data Subject?",
            "Will the processing involve automated processing of personal data, including profiling of the Data Subject?",
            "Will the processing have legal consequences or serious impact on the Data Subject?"
    );

    public static final List<Question> QUESTIONS = List.of(
            new Question(1, "General",
                    "Describe the processing activity? Provide a clear step-by-step explanation of the processing operations and how you will collect and use the data."),
            new Question(2, "Purpose Limitation", "What is the purpose of the processing activity?"),
            new Question(3, "Data Minimization",
                    "What types of personal data and/or sensitive personal data will be processed?"),
            new Question(4, "Security of personal data", "List of information systems"),
            new Question(5, "Accountability", "Does this processing activity included in RoPA?"),
            new Question(6, "Transparency", "How do you inform individuals about processing of personal data?"),
            new Question(7, "Data Minimization",
                    "How will you ensure only necessary personal data is processed?"),
            new Question(8, "Fairness",
                    "Is processing of personal data necessary for the intended purpose? Could the same purpose be achieved with less intrusive means?"),
            new Question(9, "Data Accuracy", "How do you ensure accuracy of personal data?"),
            new Question(10, "Lawfulness", "What is the legal basis of processing the personal data?"),
            new Question(11, "Fairness",
                    "If you chose Consent as a legal basis, what measures have you implemented to comply with Consent requirements under the UAE PDPL?"),
            new Question(12, "Third Party & Cross Border",
                    "Will personal data be shared with third parties or transferred across borders? Describe safeguards."),
            new Question(13, "Storage limitation", "How long will personal data be retained and what is the deletion/anonymisation approach?"),
            new Question(14, "Security of personal data",
                    "What technical and organisational security measures protect the personal data?"),
            new Question(15, "Data Subject Rights",
                    "How will data subject rights (access, correction, deletion, etc.) be handled for this processing?"),
            new Question(16, "Accountability",
                    "What records, policies, or training support accountability for this processing?"),
            new Question(17, "Risks",
                    "What privacy risks have you identified and what mitigations are planned?")
    );

    public static final List<Principle> PRINCIPLES = List.of(
            new Principle(1, "Lawfulness", "Lawful basis must be in place for processing and disclosure."),
            new Principle(2, "Purpose limitation", "Specific purpose for processing; no incompatible further use."),
            new Principle(3, "Transparency", "Open and clear information to data subjects about processing."),
            new Principle(4, "Fairness", "Process only in ways data subjects would reasonably expect."),
            new Principle(5, "Data minimization", "Collect and process only the minimum personal data needed."),
            new Principle(6, "Storage limitation", "Retain only for a specified period; then delete or anonymise."),
            new Principle(7, "Accuracy", "Reasonable measures to keep personal data accurate and up to date."),
            new Principle(8, "Security", "Appropriate technical and organisational measures."),
            new Principle(9, "Accountability", "Measures and records demonstrating compliance."),
            new Principle(10, "Data Subject Rights", "Handle access, correction, deletion, and related rights."),
            new Principle(11, "Third Party and Cross Border Transfer",
                    "Contracts and safeguards for processors and cross-border transfers.")
    );

    public static final List<String> LIKELIHOOD = List.of(
            "Rare", "Unlikely", "Possible", "Likely", "Almost Certain");
    public static final List<String> IMPACT = List.of(
            "Very Low", "Low", "Medium", "High", "Very High");
    public static final List<String> TREATMENT = List.of(
            "Accept", "Treat", "Transfer", "Terminate");

    private DpiaMethodologyBank() {
    }
}
