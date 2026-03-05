/**
 * PharmaManagement — Billing Page JS  (v2.1 — fixed)
 *
 * ROOT CAUSE OF EMPTY BILLS:
 *   1. readonly inputs are not reliably submitted in all browsers.
 *      Fix → each computed field has a paired hidden input that gets
 *             its value set explicitly before the form submits.
 *   2. The autocomplete dropdown "mousedown" was firing AFTER the
 *      input's "blur", causing the dropdown to close before the
 *      click registered. Fix → preventDefault() on mousedown.
 *   3. Row index must be globally unique (not reset) so that
 *      Thymeleaf list binding items[0], items[1]... never collides
 *      if rows are added/removed.
 */

const fmt   = (n) => parseFloat(n || 0).toFixed(2);
const rupee = (n) => '&#8377;' + fmt(n);   // ₹ as HTML entity — safe in all contexts

let rowIndex = 0;   // global, never reset — guarantees unique field names

// ════════════════════════════════════════════════════════════════════
//  ADD ROW
// ════════════════════════════════════════════════════════════════════
function addItemRow() {
  const tbody = document.getElementById('billing-items-tbody');
  const idx   = rowIndex++;

  const tr = document.createElement('tr');
  tr.setAttribute('data-row', idx);

  tr.innerHTML = `
    <td class="item-num-cell">${tbody.children.length + 1}</td>

    <!-- Medicine (autocomplete) -->
    <td style="min-width:200px">
      <div class="autocomplete-wrapper">
        <input type="hidden"  name="items[${idx}].medicineId"   id="medId_${idx}" />
        <input type="text"
               class="form-control med-name-input"
               name="items[${idx}].medicineName"
               placeholder="Search medicine..."
               autocomplete="off"
               id="medName_${idx}"
               data-idx="${idx}"
               oninput="onMedSearch(this)"
               onfocus="onMedSearch(this)"
               onblur="delayCloseDropdown(${idx})" />
        <div class="autocomplete-list" id="acList_${idx}"></div>
      </div>
    </td>

    <!-- Batch No -->
    <td style="min-width:100px">
      <input type="text"
             class="form-control"
             name="items[${idx}].batchNo"
             id="batchNo_${idx}"
             placeholder="Auto" />
    </td>

    <!-- Qty -->
    <td style="width:75px">
      <input type="number"
             class="form-control"
             name="items[${idx}].quantity"
             id="qty_${idx}"
             value="1" min="1"
             data-idx="${idx}"
             oninput="calcRow(${idx})" />
    </td>

    <!-- Unit Price -->
    <td style="width:105px">
      <input type="number"
             class="form-control"
             name="items[${idx}].unitPrice"
             id="price_${idx}"
             placeholder="0.00" step="0.01" min="0"
             data-idx="${idx}"
             oninput="calcRow(${idx})" />
    </td>

    <!-- GST % -->
    <td style="width:80px">
      <input type="number"
             class="form-control"
             name="items[${idx}].gstPercentage"
             id="gst_${idx}"
             placeholder="0" step="0.01" min="0" max="100"
             data-idx="${idx}"
             oninput="calcRow(${idx})" />
    </td>

    <!-- Item Total (display only + hidden submit) -->
    <td style="width:105px">
      <input type="text" class="form-control" id="itemTotalDisp_${idx}"
             placeholder="0.00" readonly
             style="background:#f8fafc;color:var(--text-muted)" />
      <input type="hidden" name="items[${idx}].itemTotal" id="itemTotal_${idx}" value="0.00" />
    </td>

    <!-- GST Amount (display only + hidden submit) -->
    <td style="width:105px">
      <input type="text" class="form-control" id="gstAmtDisp_${idx}"
             placeholder="0.00" readonly
             style="background:#f8fafc;color:var(--text-muted)" />
      <input type="hidden" name="items[${idx}].gstAmount" id="gstAmt_${idx}" value="0.00" />
    </td>

    <!-- Row Total (display only + hidden submit) -->
    <td style="width:110px">
      <input type="text" class="form-control" id="rowTotalDisp_${idx}"
             placeholder="0.00" readonly
             style="background:#f8fafc;color:var(--primary);font-weight:700" />
      <input type="hidden" name="items[${idx}].totalAmount" id="rowTotal_${idx}" value="0.00" />
    </td>

    <!-- Remove -->
    <td style="width:40px;text-align:center">
      <button type="button" class="btn btn-sm btn-danger btn-icon"
              onclick="removeRow(this)" title="Remove row">&#10005;</button>
    </td>
  `;

  tbody.appendChild(tr);
  renumberRows();
}

// ════════════════════════════════════════════════════════════════════
//  REMOVE ROW
// ════════════════════════════════════════════════════════════════════
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

function renumberRows() {
  document.querySelectorAll('#billing-items-tbody tr').forEach((tr, i) => {
    const cell = tr.querySelector('.item-num-cell');
    if (cell) cell.textContent = i + 1;
  });
}

// ════════════════════════════════════════════════════════════════════
//  CALCULATE ROW
// ════════════════════════════════════════════════════════════════════
function calcRow(idx) {
  const qty    = parseFloat(getVal(`qty_${idx}`)   || 0);
  const price  = parseFloat(getVal(`price_${idx}`) || 0);
  const gstPct = parseFloat(getVal(`gst_${idx}`)   || 0);

  const itemTotal = qty * price;
  const gstAmt    = itemTotal * gstPct / 100;
  const rowTotal  = itemTotal + gstAmt;

  // Update display inputs
  setDisp(`itemTotalDisp_${idx}`, itemTotal);
  setDisp(`gstAmtDisp_${idx}`,   gstAmt);
  setDisp(`rowTotalDisp_${idx}`, rowTotal);

  // Update hidden submit inputs — these are what Spring actually receives
  setHidden(`itemTotal_${idx}`, itemTotal);
  setHidden(`gstAmt_${idx}`,    gstAmt);
  setHidden(`rowTotal_${idx}`,  rowTotal);

  recalcSummary();
}

// ════════════════════════════════════════════════════════════════════
//  BILL SUMMARY
// ════════════════════════════════════════════════════════════════════
function recalcSummary() {
  let subtotal = 0, totalGst = 0;

  document.querySelectorAll('#billing-items-tbody tr').forEach(tr => {
    const idx = tr.getAttribute('data-row');
    subtotal += parseFloat(getVal(`itemTotal_${idx}`) || 0);
    totalGst += parseFloat(getVal(`gstAmt_${idx}`)   || 0);
  });

  const cgst       = totalGst / 2;
  const sgst       = totalGst / 2;
  const grandTotal = subtotal + totalGst;

  // Update summary display
  setHtml('sumSubtotal',  rupee(subtotal));
  setHtml('sumCgst',      rupee(cgst));
  setHtml('sumSgst',      rupee(sgst));
  setHtml('sumTotalGst',  rupee(totalGst));
  setHtml('sumGrandTotal',rupee(grandTotal));

  // Push values into the hidden form inputs submitted to Spring
  setHidden('hSubtotal',   subtotal);
  setHidden('hTotalGst',   totalGst);
  setHidden('hCgst',       cgst);
  setHidden('hSgst',       sgst);
  setHidden('hGrandTotal', grandTotal);
}

// ════════════════════════════════════════════════════════════════════
//  MEDICINE AUTOCOMPLETE
// ════════════════════════════════════════════════════════════════════
let searchTimer = null;

function onMedSearch(input) {
  clearTimeout(searchTimer);
  const idx  = input.getAttribute('data-idx');
  const q    = input.value.trim();
  const list = document.getElementById(`acList_${idx}`);

  if (q.length < 1) { closeDropdown(idx); return; }

  searchTimer = setTimeout(() => {
    fetch('/billing/medicine/search?q=' + encodeURIComponent(q))
      .then(r => r.json())
      .then(meds => renderAutocomplete(meds, idx, list))
      .catch(() => closeDropdown(idx));
  }, 200);
}

function renderAutocomplete(meds, idx, list) {
  list.innerHTML = '';
  if (!meds || !meds.length) { closeDropdown(idx); return; }

  meds.forEach(med => {
    const div = document.createElement('div');
    div.className = 'autocomplete-item';

    // Use innerHTML with entities to avoid special character issues
    const name    = escHtml(med.name   || '');
    const gstPct  = med.gstPercentage  != null ? med.gstPercentage : 0;
    const price   = parseFloat(med.price || 0).toFixed(2);

    div.innerHTML =
      '<span><strong>' + name + '</strong> ' +
      '<span class="ac-gst">' + gstPct + '% GST</span></span>' +
      '<span class="ac-price">&#8377;' + price + '</span>';

    // mousedown fires before blur — preventDefault stops dropdown from
    // closing before the click is processed (critical fix)
    div.addEventListener('mousedown', function(e) {
      e.preventDefault();
      selectMedicine(med, idx);
    });

    list.appendChild(div);
  });

  list.classList.add('open');
}

function selectMedicine(med, idx) {
  // Visible search input
  setById(`medName_${idx}`, med.name || '');

  // Hidden bound inputs — these bind to BillingItemForm fields
  setById(`medId_${idx}`,         med.id       || '');
  setById(`batchNo_${idx}`,       med.batchNo  || '');
  setById(`price_${idx}`,         parseFloat(med.price          || 0).toFixed(2));
  setById(`gst_${idx}`,           parseFloat(med.gstPercentage  || 0).toFixed(2));

  closeDropdown(idx);
  calcRow(idx);
}

// Delay close so mousedown click has time to register on slow devices
function delayCloseDropdown(idx) {
  setTimeout(() => closeDropdown(idx), 200);
}

function closeDropdown(idx) {
  const list = document.getElementById(`acList_${idx}`);
  if (list) { list.classList.remove('open'); list.innerHTML = ''; }
}

// Close all dropdowns on outside click
document.addEventListener('click', function(e) {
  if (!e.target.closest('.autocomplete-wrapper')) {
    document.querySelectorAll('.autocomplete-list')
            .forEach(l => l.classList.remove('open'));
  }
});

// ════════════════════════════════════════════════════════════════════
//  HELPERS
// ════════════════════════════════════════════════════════════════════
function getVal(id)       { const el = document.getElementById(id); return el ? el.value : ''; }
function setById(id, val) { const el = document.getElementById(id); if (el) el.value = val; }
function setDisp(id, num) { const el = document.getElementById(id); if (el) el.value = parseFloat(num).toFixed(2); }
function setHidden(id, num) {
  const el = document.getElementById(id);
  if (el) el.value = parseFloat(num).toFixed(2);
}
function setHtml(id, html) { const el = document.getElementById(id); if (el) el.innerHTML = html; }
function escHtml(str) {
  return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

// ════════════════════════════════════════════════════════════════════
//  INIT
// ════════════════════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', function() {
  addItemRow();    // Add first empty row
  recalcSummary(); // Initialise summary to ₹0.00
});
