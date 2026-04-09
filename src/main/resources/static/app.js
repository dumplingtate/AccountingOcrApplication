const API_URL = "/api/documents/upload";
const COUNTERPARTIES_URL = "/api/counterparties";
const uploadedDocuments = [];

function hideAllScreens() {
    ["main-screen", "documents-screen", "counterparties-screen"].forEach((id) => {
        const el = document.getElementById(id);
        if (el) {
            el.classList.add("hidden");
        }
    });
}

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
    hideAllScreens();
    document.getElementById("main-screen").classList.remove("hidden");
}

function showDocumentsScreen() {
    hideAllScreens();
    document.getElementById("documents-screen").classList.remove("hidden");
    renderDocumentsList();
}

function showCounterpartiesScreen() {
    hideAllScreens();
    document.getElementById("counterparties-screen").classList.remove("hidden");
    loadCounterparties();
}

function escapeHtml(value) {
    if (value == null || value === "") {
        return "-";
    }
    const div = document.createElement("div");
    div.textContent = value;
    return div.innerHTML;
}

async function loadCounterparties() {
    const statusEl = document.getElementById("counterpartiesStatus");
    const wrap = document.getElementById("counterpartiesTableWrap");
    if (!statusEl || !wrap) {
        return;
    }

    statusEl.innerText = "Загрузка…";
    wrap.innerHTML = "";

    try {
        const response = await fetch(COUNTERPARTIES_URL);
        const text = await response.text();
        if (!response.ok) {
            throw new Error(text || "HTTP " + response.status);
        }
        let list;
        try {
            list = JSON.parse(text);
        } catch {
            throw new Error("Ответ не JSON:\n" + text);
        }
        if (!Array.isArray(list)) {
            throw new Error("Ожидался массив контрагентов");
        }
        if (list.length === 0) {
            statusEl.innerText = "В базе пока нет контрагентов.";
            wrap.innerHTML = "<p>Данные появятся после обработки документов с ИНН контрагента.</p>";
            return;
        }
        statusEl.innerText = "Найдено записей: " + list.length;
        const rows = list
            .map(
                (c) =>
                    `<tr>
            <td>${escapeHtml(c.id)}</td>
            <td>${escapeHtml(c.name)}</td>
            <td>${escapeHtml(c.inn)}</td>
            <td>${escapeHtml(c.kpp)}</td>
            <td>${escapeHtml(c.legalAddress)}</td>
            <td>${escapeHtml(c.phone)}</td>
            <td>${escapeHtml(c.email)}</td>
          </tr>`
            )
            .join("");
        wrap.innerHTML =
            `<table class="cp-table"><thead><tr>
            <th>ID</th><th>Наименование</th><th>ИНН</th><th>КПП</th><th>Адрес</th><th>Телефон</th><th>Email</th>
          </tr></thead><tbody>${rows}</tbody></table>`;
    } catch (e) {
        statusEl.innerText = "Ошибка: " + (e.message || e);
    }
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
