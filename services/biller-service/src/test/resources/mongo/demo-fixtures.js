for (const [id, type] of [["water-demo", "WATER"], ["power-demo", "ELECTRICITY"], ["telecom-demo", "TELECOM"]]) {
    db.billers.updateOne({ _id: id }, { $setOnInsert: { type } }, { upsert: true });
}

db.invoices.updateOne(
    { billerId: "water-demo", invoiceRef: "demo-001" },
    { $setOnInsert: { version: NumberLong("1"), amountMinor: NumberLong("14025"), currency: "MAD", status: "OPEN" } },
    { upsert: true }
);
