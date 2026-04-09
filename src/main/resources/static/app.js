const API_URL = "/api/documents/upload";
const uploadedDocuments = [];

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

function showMainScreen() {
    document.getElementById("main-screen").classList.remove("hidden");
    document.getElementById("documents-screen").classList.add("hidden");
}

function showDocumentsScreen() {
    document.getElementById("main-screen").classList.add("hidden");
    document.getElementById("documents-screen").classList.remove("hidden");
    renderDocumentsList();
}

function renderDocumentsList() {
    const list = document.getElementById("documentsList");
    if (!list) {
        return;
    }

    if (uploadedDocuments.length === 0) {
        list.innerHTML = "<p>Пока нет добавленных документов</p>";
        return;
    }

    list.innerHTML = uploadedDocuments.map((doc, index) => `
        <div class="doc-item">
            <div><b>#${index + 1}</b></div>
            <div><b>ID:</b> ${doc.id || doc.fileId || "-"}</div>
            <div><b>Тип:</b> ${doc.documentType || "-"}</div>
            <div><b>Добавлен:</b> ${doc.addedAt}</div>
        </div>
    `).join("");
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

        let data;
        try {
            data = JSON.parse(text);
        } catch {
            throw new Error("Сервер вернул не JSON:\n" + text);
        }

        status.innerText = "Готово";

        // ←←← ТОЛЬКО ЭТИ ДВЕ СТРОКИ + raw
        document.getElementById("id").innerText = data.id || data.fileId || "-";
        document.getElementById("type").innerText = data.documentType || "-";

        document.getElementById("raw").innerText = JSON.stringify(data, null, 2);
        uploadedDocuments.unshift({
            ...data,
            addedAt: new Date().toLocaleString("ru-RU")
        });
        renderDocumentsList();

    } catch (e) {
        status.innerText = "Ошибка";
        document.getElementById("raw").innerText = e.message;
    }
}
