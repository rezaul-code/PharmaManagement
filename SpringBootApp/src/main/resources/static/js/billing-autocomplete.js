// billing-autocomplete.js
'use strict';

document.addEventListener('DOMContentLoaded', function () {

    // Renumber row field names: items[i].medicineName, items[i].medicineId, items[i].quantity
    function renumberRows() {
        const rows = document.querySelectorAll('#medicineTable tbody tr');
        rows.forEach((row, i) => {
            const nameInput = row.querySelector('.medicineInput');
            const idInput = row.querySelector('.medicineId');
            const qty = row.querySelector('input[type="number"]');

            if (nameInput) nameInput.name = `items[${i}].medicineName`;
            if (idInput) idInput.name = `items[${i}].medicineId`;
            if (qty) qty.name = `items[${i}].quantity`;
        });
    }

    // Add a new row
    const addRowBtn = document.getElementById("addRow");
    if (addRowBtn) {
        addRowBtn.addEventListener("click", function () {
            const tbody = document.querySelector("#medicineTable tbody");

            const tr = document.createElement("tr");
            const index = tbody.querySelectorAll("tr").length;

            tr.innerHTML = `
                <td>
                    <input type="text" name="items[${index}].medicineName"
                           class="medicineInput" required autocomplete="off" />
                    <input type="hidden" name="items[${index}].medicineId" class="medicineId" />
                    <datalist id="medicineList${index}"></datalist>
                </td>
                <td>
                    <input type="number" name="items[${index}].quantity" min="1" value="1" required>
                </td>
                <td>
                    <button type="button" class="removeRow">X</button>
                </td>
            `;
            tbody.appendChild(tr);
            renumberRows();
        });
    }

    // Remove row handler
    document.addEventListener("click", function (e) {
        if (e.target && e.target.classList.contains("removeRow")) {
            const row = e.target.closest("tr");
            if (row) {
                row.remove();
                renumberRows();
            }
        }
    });

    // Autocomplete (search by name or id)
    document.addEventListener("input", function (e) {
        if (!e.target || !e.target.classList.contains("medicineInput")) return;

        let query = e.target.value.toLowerCase();
        let datalistId = e.target.closest("td").querySelector("datalist").id;
        let datalist = document.getElementById(datalistId);
        datalist.innerHTML = "";

        if (query.length === 0) return;

        medicines
            .filter(m =>
                m.name.toLowerCase().startsWith(query) ||
                m.id.toString().startsWith(query)
            )
            .forEach(m => {
                let option = document.createElement("option");
                option.value = m.name; 
                option.textContent = `${m.name} | ID: ${m.id} | Stock: ${m.quantity}`;
                option.setAttribute("data-id", m.id);
                datalist.appendChild(option);
            });

        e.target.setAttribute("list", datalistId);
    });

    // Copy selected ID into hidden input
    document.addEventListener("change", function (e) {
        if (!e.target || !e.target.classList.contains("medicineInput")) return;

        const input = e.target;
        const datalist = document.getElementById(input.getAttribute("list"));
        const options = datalist.querySelectorAll("option");
        const hidden = input.closest("tr").querySelector(".medicineId");

        let match = null;
        options.forEach(opt => {
            if (opt.value === input.value) {
                match = opt;
            }
        });

        if (match) {
            hidden.value = match.getAttribute("data-id");
            input.classList.remove("invalid");
        } else {
            hidden.value = "";
        }
    });

    // Validate on submit
    const form = document.querySelector('form[method="post"]');
    if (form) {
        form.addEventListener('submit', function (e) {
            let invalid = false;
            document.querySelectorAll('#medicineTable tbody tr').forEach(row => {
                const hid = row.querySelector('.medicineId');
                const nameInput = row.querySelector('.medicineInput');
                if (!hid || !hid.value) {
                    invalid = true;
                    if (nameInput) nameInput.classList.add('invalid');
                } else {
                    if (nameInput) nameInput.classList.remove('invalid');
                }
            });
            if (invalid) {
                e.preventDefault();
                alert('⚠ Please select valid medicines from suggestions (name or ID).');
            }
        });
    }

    renumberRows();
});
