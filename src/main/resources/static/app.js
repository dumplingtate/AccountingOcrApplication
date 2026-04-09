const API_URL = "/api/documents/upload";

function login() {
    if (
        document.getElementById("login").value === "user" &&
        document.getElementById("password").value === "1234"
    ) {
        document.getElementById("login-page").classList.add("hidden");
        document.getElementById("app").classList.remove("hidden");
    } else {
        document.getElementById("error").innerText = "Ошибка входа";
    }
}

async function upload() {
    const file = document.getElementById("fileInput").files[0];
    const status = document.getElementById("status");

    if (!file) {
        alert("Выбери файл");
        return;
    }

    status.innerText = "Обработка...";

    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch(API_URL, {
            method: "POST",
            body: formData
        });

        const text = await response.text();

        // 🔥 фикс ошибки JSON
        let data;
        try {
            data = JSON.parse(text);
        } catch {
            throw new Error("Сервер вернул не JSON:\n" + text);
        }

        status.innerText = "Готово";

        // 👇 адаптируй под свой DTO
        document.getElementById("type").innerText = data.type || "-";
        document.getElementById("date").innerText = data.date || "-";
        document.getElementById("amount").innerText = data.amount || "-";
        document.getElementById("inn").innerText = data.inn || "-";
        document.getElementById("id").innerText = data.id || "-";
        document.getElementById("filename").innerText = data.fileName || "-";
        document.getElementById("type").innerText = data.documentType || "-";
        document.getElementById("confidence").innerText =
            data.confidence ? (data.confidence * 100).toFixed(1) + "%" : "-";

        document.getElementById("date").innerText =
            data.extractedData?.date || "-";

        document.getElementById("amount").innerText =
            data.extractedData?.amount || "-";

        document.getElementById("inn").innerText =
            data.extractedData?.inn || "-";

        document.getElementById("raw").innerText =
            JSON.stringify(data, null, 2);

    } catch (e) {
        status.innerText = "Ошибка";
        document.getElementById("raw").innerText = e.message;
    }
}