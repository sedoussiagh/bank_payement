const hello = db.hello();
if (hello.setName && hello.setName !== "biller-rs") {
    throw new Error("Unexpected replica set: " + hello.setName);
}
if (!hello.setName) {
    rs.initiate({
        _id: "biller-rs",
        members: [{ _id: 0, host: "mongo:27017" }]
    });
}
