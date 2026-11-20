db.invoices.createIndex(
    { billerId: 1, invoiceRef: 1 },
    { name: "invoice_identity", unique: true }
);
db.biller_decisions.createIndex(
    { decisionDueAt: 1 },
    { name: "pending_decision_due", partialFilterExpression: { status: "PENDING" } }
);
