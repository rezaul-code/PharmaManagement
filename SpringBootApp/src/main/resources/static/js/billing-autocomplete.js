/**
 * PharmaManagement — Billing Page JS
 * Handles: medicine autocomplete, GST calculation, row management
 */

// ── Utility ─────────────────────────────────────────────────────────

const fmt = (n) => parseFloat(n || 0).toFixed(2);
const rupee = (n) => '₹' + fmt(n);

// ── Row counter (keeps unique index for Thymeleaf list binding) ──────
let rowIndex = 0;

// ── Add a new billing item row ────────────────────────────────────────
function addItemRow() {
  const tbody = document.getElementById('billing-items-tbody');
  const idx   = rowIndex++;

  const tr = document.createElement('tr');
  tr.setAttribute('data-row', idx);
  tr.innerHTML = `
    <td class="item-num-cell">${tbody.children.length + 1}</td>

    <!-- Medicine Name + autocomplete -->
    <td style="min-width:180px">
      <div class="autocomplete-wrapper">
        <input type="hidden"
               name="items[${idx}].medicineId"
               id="medId_${idx}" />
        <input type="text"
               class="form-control med-name-input"
               placeholder="Search medicine…"
               autocomplete="off"
               data-idx="${idx}"
               oninput="onMedSearch(this)"
               onfocus="onMedSearch(this)"
               id="medName_${idx}" />
        <input type="hidden"
               name="items[${idx}].medicineName"
               id="medNameHidden_${idx}" />
        <div class="autocomplete-list" id="acList_${idx}"></div>
      </div>
    </td>

    <!-- Batch No -->
    <td style="min-width:100px">
      <input type="text"
             class="form-control"
             name="items[${idx}].batchNo"
             id="batchNo_${idx}"
             placeholder="—" />
    </td>

    <!-- Qty -->
    <td style="width:80px">
      <input type="number"
             class="form-control"
             name="items[${idx}].quantity"
             id="qty_${idx}"
             value="1" min="1"
             data-idx="${idx}"
             oninput="calcRow(${idx})" />
    </td>

    <!-- Unit Price -->
    <td style="width:110px">
      <input type="number"
             class="form-control"
             name="items[${idx}].unitPrice"
             id="price_${idx}"
             placeholder="0.00" step="0.01" min="0"
             data-idx="${idx}"
             oninput="calcRow(${idx})" />
    </td>

    <!-- GST % -->
    <td style="width:90px">
      <input type="number"
             class="form-control"
             name="items[${idx}].gstPercentage"
             id="gst_${idx}"
             placeholder="0" step="0.01" min="0" max="100"
             data-idx="${idx}"
             oninput="calcRow(${idx})" />
    </td>

    <!-- Item Total (readonly) -->
    <td style="width:110px">
      <input type="number"
             class="form-control"
             name="items[${idx}].itemTotal"
             id="itemTotal_${idx}"
             placeholder="0.00" step="0.01"
             readonly />
    </td>

    <!-- GST Amount (readonly) -->
    <td style="width:110px">
      <input type="number"
             class="form-control"
             name="items[${idx}].gstAmount"
             id="gstAmt_${idx}"
             placeholder="0.00" step="0.01"
             readonly />
    </td>

    <!-- Row Total (readonly) -->
    <td style="width:115px">
      <input type="number"
             class="form-control font-mono fw-bold"
             name="items[${idx}].totalAmount"
             id="rowTotal_${idx}"
             placeholder="0.00" step="0.01"
             readonly style="color:var(--primary)" />
    </td>

    <!-- Delete -->
    <td style="width:44px; text-align:center">
      <button type="button"
              class="btn btn-sm btn-danger btn-icon"
              onclick="removeRow(this)"
              title="Remove">✕</button>
    </td>
  `;

  tbody.appendChild(tr);
  renumberRows();
}

// ── Remove row ────────────────────────────────────────────────────────
function removeRow(btn) {
  const tbody = document.getElementById('billing-items-tbody');
  if (tbody.children.length <= 1) {
    alert('A bill must have at least one item.');
    return;
  }
  btn.closest('tr').remove();
  renumberRows();
  recalcSummary();
}

// ── Renumber the # column ─────────────────────────────────────────────
function renumberRows() {
  const rows = document.querySelectorAll('#billing-items-tbody tr');
  rows.forEach((tr, i) => {
    const cell = tr.querySelector('.item-num-cell');
    if (cell) cell.textContent = i + 1;
  });
}

// ── Calculate a single row ────────────────────────────────────────────
function calcRow(idx) {
  const qty      = parseFloat(document.getElementById(`qty_${idx}`)?.value   || 0);
  const price    = parseFloat(document.getElementById(`price_${idx}`)?.value  || 0);
  const gstPct   = parseFloat(document.getElementById(`gst_${idx}`)?.value    || 0);

  const itemTotal = qty * price;
  const gstAmt    = itemTotal * gstPct / 100;
  const rowTotal  = itemTotal + gstAmt;

  const setVal = (id, v) => { const el = document.getElementById(id); if (el) el.value = v.toFixed(2); };
  setVal(`itemTotal_${idx}`, itemTotal);
  setVal(`gstAmt_${idx}`,    gstAmt);
  setVal(`rowTotal_${idx}`,  rowTotal);

  recalcSummary();
}

// ── Recalculate bill-level summary ────────────────────────────────────
function recalcSummary() {
  let subtotal = 0, totalGst = 0;

  document.querySelectorAll('#billing-items-tbody tr').forEach(tr => {
    const idx = tr.getAttribute('data-row');
    subtotal += parseFloat(document.getElementById(`itemTotal_${idx}`)?.value || 0);
    totalGst += parseFloat(document.getElementById(`gstAmt_${idx}`)?.value    || 0);
  });

  const cgst       = totalGst / 2;
  const sgst       = totalGst / 2;
  const grandTotal = subtotal + totalGst;

  // Update display
  setText('sumSubtotal', rupee(subtotal));
  setText('sumCgst',     rupee(cgst));
  setText('sumSgst',     rupee(sgst));
  setText('sumTotalGst', rupee(totalGst));
  setText('sumGrandTotal', rupee(grandTotal));

  // Push into hidden inputs for form submission
  setInput('hSubtotal',   subtotal);
  setInput('hTotalGst',   totalGst);
  setInput('hCgst',       cgst);
  setInput('hSgst',       sgst);
  setInput('hGrandTotal', grandTotal);
}

function setText(id, val)  { const el = document.getElementById(id); if (el) el.textContent = val; }
function setInput(id, val) { const el = document.getElementById(id); if (el) el.value = val.toFixed(2); }

// ── Medicine Autocomplete ─────────────────────────────────────────────
let searchTimer = null;

function onMedSearch(input) {
  clearTimeout(searchTimer);
  const idx = input.getAttribute('data-idx');
  const q   = input.value.trim();

  const list = document.getElementById(`acList_${idx}`);

  if (q.length < 1) { list.classList.remove('open'); list.innerHTML = ''; return; }

  searchTimer = setTimeout(() => {
    fetch(`/billing/medicine/search?q=${encodeURIComponent(q)}`)
      .then(r => r.json())
      .then(meds => renderAutocomplete(meds, idx, list))
      .catch(() => { list.classList.remove('open'); });
  }, 220);
}

function renderAutocomplete(meds, idx, list) {
  list.innerHTML = '';
  if (!meds.length) { list.classList.remove('open'); return; }

  meds.forEach(med => {
    const div = document.createElement('div');
    div.className = 'autocomplete-item';
    div.innerHTML = `
      <span>
        <strong>${med.name}</strong>
        <span class="ac-gst ms-1">${med.gstPercentage ?? 0}% GST</span>
      </span>
      <span class="ac-price">₹${parseFloat(med.price || 0).toFixed(2)}</span>
    `;
    div.addEventListener('mousedown', (e) => {
      e.preventDefault();
      selectMedicine(med, idx);
    });
    list.appendChild(div);
  });

  list.classList.add('open');
}

function selectMedicine(med, idx) {
  // Fill visible name input
  const nameInput = document.getElementById(`medName_${idx}`);
  if (nameInput) nameInput.value = med.name;

  // Fill hidden fields
  setInputById(`medId_${idx}`,         med.id);
  setInputById(`medNameHidden_${idx}`, med.name);
  setInputById(`batchNo_${idx}`,       med.batchNo || '');
  setInputById(`price_${idx}`,         parseFloat(med.price || 0).toFixed(2));
  setInputById(`gst_${idx}`,           parseFloat(med.gstPercentage || 0).toFixed(2));

  // Close dropdown
  const list = document.getElementById(`acList_${idx}`);
  if (list) { list.classList.remove('open'); list.innerHTML = ''; }

  // Calculate immediately
  calcRow(idx);
}

function setInputById(id, val) {
  const el = document.getElementById(id);
  if (el) el.value = val;
}

// ── Close autocomplete when clicking elsewhere ────────────────────────
document.addEventListener('click', (e) => {
  if (!e.target.closest('.autocomplete-wrapper')) {
    document.querySelectorAll('.autocomplete-list').forEach(l => l.classList.remove('open'));
  }
});

// ── Init: add first row on load ────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  addItemRow();
  recalcSummary();
});