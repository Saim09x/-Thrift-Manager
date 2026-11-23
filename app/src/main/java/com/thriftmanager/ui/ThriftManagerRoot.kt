package com.thriftmanager.ui

import android.content.ContentValues
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thriftmanager.data.Customer
import com.thriftmanager.data.Expense
import com.thriftmanager.data.Payment
import com.thriftmanager.data.Product
import com.thriftmanager.data.Purchase
import com.thriftmanager.data.Sale
import com.thriftmanager.data.Supplier
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private val Ink = Color(0xFF25313A)
private val Muted = Color(0xFF76828B)
private val CanvasGray = Color(0xFFE9EDF0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThriftManagerRoot(viewModel: MainViewModel = hiltViewModel()) {
    val products by viewModel.business.products.collectAsStateWithLifecycle(emptyList())
    val suppliers by viewModel.business.suppliers.collectAsStateWithLifecycle(emptyList())
    val customers by viewModel.business.customers.collectAsStateWithLifecycle(emptyList())
    val purchases by viewModel.business.purchases.collectAsStateWithLifecycle(emptyList())
    val sales by viewModel.business.sales.collectAsStateWithLifecycle(emptyList())
    val expenses by viewModel.business.expenses.collectAsStateWithLifecycle(emptyList())
    val payments by viewModel.business.payments.collectAsStateWithLifecycle(emptyList())
    val message by viewModel.message.collectAsStateWithLifecycle()
    val onboarded by viewModel.onboardingDone.collectAsStateWithLifecycle()
    val periods = remember { viewModel.reportPeriods() }
    val dailySales by viewModel.business.salesTotal(periods.first.first, periods.first.last + 1)
        .collectAsStateWithLifecycle(0L)
    val dailyExpenses by viewModel.business.expenseTotal(periods.first.first, periods.first.last + 1)
        .collectAsStateWithLifecycle(0L)
    val dailyGross by viewModel.business.grossProfit(periods.first.first, periods.first.last + 1)
        .collectAsStateWithLifecycle(0L)
    val monthlyExpenses by viewModel.business.expenseTotal(periods.second.first, periods.second.last + 1)
        .collectAsStateWithLifecycle(0L)
    val monthlyGross by viewModel.business.grossProfit(periods.second.first, periods.second.last + 1)
        .collectAsStateWithLifecycle(0L)
    val inventoryValue by viewModel.business.inventoryValue.collectAsStateWithLifecycle(0L)
    val productCount by viewModel.business.productCount.collectAsStateWithLifecycle(0)
    val lowStock by viewModel.business.lowStockCount.collectAsStateWithLifecycle(0)
    val damaged by viewModel.business.damagedStockCount.collectAsStateWithLifecycle(0)
    val supplierDue by viewModel.business.supplierDue.collectAsStateWithLifecycle(0L)
    val customerDue by viewModel.business.customerDue.collectAsStateWithLifecycle(0L)

    var selected by remember { mutableStateOf("Home") }
    var moreRoute by remember { mutableStateOf<String?>(null) }
    var restoreConfirm by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var receiptTarget by remember { mutableStateOf<Sale?>(null) }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = viewModel.backupJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: error("Could not write the backup file.")
            }.onFailure { snackbar.showSnackbar(it.message ?: "Backup export failed.") }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read the selected file.")
                viewModel.restoreJson(json)
            }.onFailure { snackbar.showSnackbar(it.message ?: "Backup restore failed.") }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val content = buildString {
                    appendLine("type,date,description,amount")
                    sales.forEach { appendLine("sale,${date(it.soldAt)},Sale #${it.id},${it.totalAmount / 100.0}") }
                    expenses.forEach { appendLine("expense,${date(it.spentAt)},${csv(it.category + " " + it.description)},${it.amount / 100.0}") }
                    purchases.forEach { appendLine("purchase,${date(it.purchasedAt)},${csv(it.invoice)},${it.totalCost / 100.0}") }
                }
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                    ?: error("Could not write the CSV file.")
            }.onFailure { snackbar.showSnackbar(it.message ?: "CSV export failed.") }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val document = PdfDocument()
                val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
                val paint = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 18f }
                page.canvas.drawText("Thrift Manager - Business Report", 40f, 60f, paint)
                paint.textSize = 13f
                listOf(
                    "Generated: ${DateFormat.getDateTimeInstance().format(Date())}",
                    "Recorded sales: ${money(sales.filterNot { it.cancelled }.sumOf { it.totalAmount })}",
                    "Recorded expenses: ${money(expenses.sumOf { it.amount })}",
                    "Inventory value: ${money(inventoryValue)}",
                    "Customer outstanding: ${money(customerDue)}",
                    "Supplier outstanding: ${money(supplierDue)}"
                ).forEachIndexed { index, text ->
                    page.canvas.drawText(text, 40f, 100f + index * 26f, paint)
                }
                document.finishPage(page)
                context.contentResolver.openOutputStream(uri)?.use(document::writeTo)
                    ?: error("Could not write the PDF file.")
                document.close()
            }.onFailure { snackbar.showSnackbar(it.message ?: "PDF export failed.") }
        }
    }
    val receiptLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val sale = receiptTarget
        if (uri != null && sale != null) scope.launch {
            runCatching {
                val lines = viewModel.business.saleLines(sale.id)
                val document = PdfDocument()
                try {
                    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
                    val paint = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 21f }
                    page.canvas.drawText("THRIFT MANAGER", 40f, 60f, paint)
                    paint.textSize = 14f
                    page.canvas.drawText("Sale receipt #${sale.id}", 40f, 95f, paint)
                    page.canvas.drawText(date(sale.soldAt), 40f, 120f, paint)
                    var y = 165f
                    lines.forEach { line ->
                        page.canvas.drawText(line.productName.take(30), 40f, y, paint)
                        page.canvas.drawText("${line.quantity} × ${money(line.unitPrice)}", 360f, y, paint)
                        y += 25f
                    }
                    y += 12f
                    listOf(
                        "Total: ${money(sale.totalAmount)}",
                        "Paid: ${money(sale.paidAmount)}",
                        "Balance: ${money(sale.totalAmount - sale.paidAmount)}",
                        "Payment: ${sale.paymentMethod}"
                    ).forEach { text -> page.canvas.drawText(text, 40f, y, paint); y += 25f }
                    document.finishPage(page)
                    context.contentResolver.openOutputStream(uri)?.use(document::writeTo)
                        ?: error("Could not write the receipt.")
                } finally {
                    document.close()
                }
            }.onFailure { snackbar.showSnackbar(it.message ?: "Receipt export failed.") }
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    if (!onboarded) {
        OnboardingScreen(onContinue = viewModel::finishOnboarding)
        return
    }

    val title = moreRoute ?: selected
    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        if (selected == "Home") Text("Your business at a glance", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                },
                navigationIcon = {
                    if (moreRoute != null) IconButton(onClick = { moreRoute = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf(
                    Triple("Home", Icons.Default.Home, "Home"),
                    Triple("Inventory", Icons.Default.Inventory2, "Inventory"),
                    Triple("Sales", Icons.Default.PointOfSale, "Sales"),
                    Triple("Reports", Icons.Default.Assessment, "Reports"),
                    Triple("More", Icons.Default.MoreHoriz, "More")
                ).forEach { (label, icon, route) ->
                    NavigationBarItem(
                        selected = selected == route && moreRoute == null,
                        onClick = { selected = route; moreRoute = null },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        when (moreRoute ?: selected) {
            "Home" -> DashboardScreen(
                Modifier.padding(padding), dailySales, dailyExpenses, dailyGross,
                monthlyGross - monthlyExpenses, inventoryValue, productCount, lowStock,
                damaged, supplierDue, customerDue, sales,
                onQuickAction = { action ->
                    selected = when (action) {
                        "New sale" -> "Sales"
                        "Add product" -> "Inventory"
                        else -> "More"
                    }
                    moreRoute = if (action == "Add expense") "Expenses" else null
                }
            )
            "Inventory" -> InventoryScreen(Modifier.padding(padding), products, suppliers, viewModel)
            "Sales" -> SalesScreen(
                Modifier.padding(padding), products, customers, sales, viewModel,
                onReceipt = { sale ->
                    receiptTarget = sale
                    receiptLauncher.launch("receipt-${sale.id}.pdf")
                }
            )
            "Reports" -> ReportsScreen(
                Modifier.padding(padding), sales, expenses, purchases, inventoryValue,
                customerDue, supplierDue,
                onCsv = { csvLauncher.launch("thrift-manager-report.csv") },
                onPdf = { pdfLauncher.launch("thrift-manager-report.pdf") }
            )
            "More" -> MoreScreen(Modifier.padding(padding)) { moreRoute = it }
            "Purchases" -> PurchasesScreen(Modifier.padding(padding), products, suppliers, purchases, viewModel)
            "Expenses" -> ExpensesScreen(Modifier.padding(padding), expenses, viewModel)
            "Suppliers" -> PartyScreen(
                Modifier.padding(padding), "Suppliers", suppliers.map { it.name to it.phone }, supplierDue,
                onAdd = { name, phone, address -> viewModel.addSupplier(Supplier(name = name, phone = phone, address = address)) }
            )
            "Customers" -> PartyScreen(
                Modifier.padding(padding), "Customers", customers.map { it.name to it.phone }, customerDue,
                onAdd = { name, phone, _ -> viewModel.addCustomer(Customer(name = name, phone = phone)) }
            )
            "Udhaar" -> DuesScreen(
                Modifier.padding(padding), sales, purchases, customers, suppliers, payments, viewModel
            )
            "Backup" -> BackupScreen(
                Modifier.padding(padding),
                onExport = { backupLauncher.launch("thrift-manager-${System.currentTimeMillis()}.json") },
                onRestore = { restoreConfirm = true },
                onCsv = { csvLauncher.launch("thrift-manager-export.csv") }
            )
            "Settings" -> SettingsScreen(Modifier.padding(padding))
        }
    }
    if (restoreConfirm) {
        AlertDialog(
            onDismissRequest = { restoreConfirm = false },
            title = { Text("Replace existing data?") },
            text = { Text("Restoring a backup replaces the business records currently stored on this device. Continue only if this is intended.") },
            confirmButton = {
                Button(onClick = {
                    restoreConfirm = false
                    restoreLauncher.launch(arrayOf("application/json", "text/*"))
                }) { Text("Choose backup") }
            },
            dismissButton = { TextButton(onClick = { restoreConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.White).padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(64.dp).background(Color(0xFFE8ECEF), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Store, null, tint = Ink, modifier = Modifier.size(32.dp)) }
        Spacer(Modifier.height(24.dp))
        Text("A clearer view of your shop.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(12.dp))
        Text(
            "Manage stock, record sales and expenses, and understand what your business earns. Your records stay on this device.",
            color = Muted,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Set up my shop") }
    }
}

@Composable
private fun DashboardScreen(
    modifier: Modifier,
    sales: Long,
    expenses: Long,
    gross: Long,
    net: Long,
    stockValue: Long,
    count: Int,
    lowStock: Int,
    damaged: Int,
    supplierDue: Long,
    customerDue: Long,
    recent: List<Sale>,
    onQuickAction: (String) -> Unit
) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(4.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Today's sales", sales, Modifier.weight(1f), positive = true)
                MetricCard("Today's expenses", expenses, Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF48535D))) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("TODAY'S GROSS PROFIT", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(money(gross), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Monthly net profit  ${money(net)}", color = Color.White.copy(alpha = .85f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Inventory value", stockValue, Modifier.weight(1f))
                InfoMetric("Products", count.toString(), Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Stock & balances", fontWeight = FontWeight.SemiBold)
                    SummaryRow("Low stock", lowStock.toString())
                    SummaryRow("Damaged pieces", damaged.toString())
                    SummaryRow("Supplier payments due", money(supplierDue))
                    SummaryRow("Customer udhaar", money(customerDue))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("New sale", "Add product", "Add expense").forEach { label ->
                    OutlinedButton(onClick = { onQuickAction(label) }, modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Recent sales", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    if (recent.isEmpty()) EmptyState("No sales yet", "Your completed sales will appear here.")
                    else recent.take(5).forEach { SaleRow(it) }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MetricCard(label: String, amount: Long, modifier: Modifier, positive: Boolean = false) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = if (positive) Color(0xFFEFF3F5) else Color.White)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text(money(amount), color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun InfoMetric(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text(value, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun SummaryRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = Muted)
        Text(value, color = Ink, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SaleRow(sale: Sale, onReceipt: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Sale #${sale.id}", fontWeight = FontWeight.Medium)
            Text("${date(sale.soldAt)} · ${sale.paymentMethod}", style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(money(sale.totalAmount), fontWeight = FontWeight.SemiBold)
            if (onReceipt != null) IconButton(onClick = onReceipt) {
                Icon(Icons.Default.Print, contentDescription = "Export receipt")
            }
        }
    }
}

@Composable
private fun InventoryScreen(modifier: Modifier, products: List<Product>, suppliers: List<Supplier>, vm: MainViewModel) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Product?>(null) }
    var dialog by remember { mutableStateOf(false) }
    val filtered = products.filter { it.name.contains(query, true) || it.sku.contains(query, true) }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search name or SKU") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            if (filtered.isEmpty()) EmptyState("No products found", "Add products to start tracking your inventory.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(filtered, key = { it.id }) { product ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), onClick = {
                        editing = product
                        dialog = true
                    }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.SemiBold)
                                Text("${product.sku} · ${product.category.ifBlank { "Uncategorized" }}", color = Muted, style = MaterialTheme.typography.labelSmall)
                                Text("Cost ${money(product.purchasePrice)}  ·  Sell ${money(product.sellingPrice)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${product.quantity} pcs", fontWeight = FontWeight.Bold)
                                if (product.quantity <= product.lowStockAt) Text("Low stock", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                if (product.damagedQuantity > 0) Text("${product.damagedQuantity} damaged", color = Muted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editing = null; dialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) { Icon(Icons.Default.Add, "Add product") }
    }
    if (dialog) ProductDialog(
        editing, suppliers,
        onDismiss = { dialog = false },
        onDelete = { productId -> vm.deleteProduct(productId); dialog = false }
    ) { product ->
        vm.saveProduct(product)
        dialog = false
    }
}

@Composable
private fun ProductDialog(
    product: Product?,
    suppliers: List<Supplier>,
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember(product) { mutableStateOf(product?.name.orEmpty()) }
    var sku by remember(product) { mutableStateOf(product?.sku.orEmpty()) }
    var category by remember(product) { mutableStateOf(product?.category.orEmpty()) }
    var brand by remember(product) { mutableStateOf(product?.brand.orEmpty()) }
    var size by remember(product) { mutableStateOf(product?.size.orEmpty()) }
    var color by remember(product) { mutableStateOf(product?.color.orEmpty()) }
    var condition by remember(product) { mutableStateOf(product?.condition ?: "Good") }
    var cost by remember(product) { mutableStateOf(product?.purchasePrice?.let(::displayAmount).orEmpty()) }
    var price by remember(product) { mutableStateOf(product?.sellingPrice?.let(::displayAmount).orEmpty()) }
    var stock by remember(product) { mutableStateOf(product?.quantity?.toString().orEmpty()) }
    var damaged by remember(product) { mutableStateOf(product?.damagedQuantity?.toString() ?: "0") }
    var lowAt by remember(product) { mutableStateOf(product?.lowStockAt?.toString() ?: "2") }
    var notes by remember(product) { mutableStateOf(product?.notes.orEmpty()) }
    var supplierId by remember(product) { mutableStateOf(product?.supplierId) }
    var supplierMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "New product" else "Edit product") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.height(440.dp)) {
                item { FormField("Product name", name, { name = it }) }
                item { FormField("SKU / barcode ID", sku, { sku = it }) }
                item { FormField("Category", category, { category = it }) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Brand", brand, { brand = it }, Modifier.weight(1f))
                    FormField("Size", size, { size = it }, Modifier.weight(1f))
                } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Color", color, { color = it }, Modifier.weight(1f))
                    FormField("Condition", condition, { condition = it }, Modifier.weight(1f))
                } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Cost (₹)", cost, { cost = it }, Modifier.weight(1f), true)
                    FormField("Selling (₹)", price, { price = it }, Modifier.weight(1f), true)
                } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Sellable qty", stock, { stock = it }, Modifier.weight(1f), true)
                    FormField("Damaged qty", damaged, { damaged = it }, Modifier.weight(1f), true)
                } }
                item { FormField("Low-stock alert at", lowAt, { lowAt = it }, keyboard = true) }
                if (suppliers.isNotEmpty()) item {
                    OutlinedButton(onClick = { supplierMenu = true }) {
                        Text(suppliers.firstOrNull { it.id == supplierId }?.name ?: "Select supplier")
                        Icon(Icons.Default.ExpandMore, null)
                    }
                    DropdownMenu(supplierMenu, { supplierMenu = false }) {
                        suppliers.forEach { supplier ->
                            DropdownMenuItem(text = { Text(supplier.name) }, onClick = { supplierId = supplier.id; supplierMenu = false })
                        }
                    }
                }
                item { FormField("Notes", notes, { notes = it }) }
                item { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            Row {
                if (product != null) TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                Button(onClick = {
                val parsedCost = parseAmount(cost)
                val parsedPrice = parseAmount(price)
                val quantity = stock.toIntOrNull()
                val damagedQuantity = damaged.toIntOrNull()
                val low = lowAt.toIntOrNull()
                when {
                    name.isBlank() || sku.isBlank() -> error = "Name and SKU are required."
                    parsedCost == null || parsedPrice == null || quantity == null || damagedQuantity == null || low == null ->
                        error = "Enter valid prices and whole-number quantities."
                    quantity < 0 || damagedQuantity < 0 || low < 0 -> error = "Stock values cannot be negative."
                    else -> onSave(Product(
                        id = product?.id ?: 0, name = name.trim(), sku = sku.trim(), category = category.trim(),
                        brand = brand.trim(), size = size.trim(), condition = condition.trim(), color = color.trim(),
                        purchasePrice = parsedCost, sellingPrice = parsedPrice, quantity = quantity,
                        damagedQuantity = damagedQuantity, lowStockAt = low, supplierId = supplierId, notes = notes
                    ))
                }
                }) { Text("Save") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete ${product?.name}?") },
        text = { Text("The product will be removed from inventory. Existing sale history remains recorded.") },
        confirmButton = {
            Button(onClick = { product?.let { onDelete(it.id) }; confirmDelete = false }) { Text("Delete product") }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } }
    )
}

@Composable
private fun SalesScreen(
    modifier: Modifier,
    products: List<Product>,
    customers: List<Customer>,
    sales: List<Sale>,
    vm: MainViewModel,
    onReceipt: (Sale) -> Unit
) {
    var showSale by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        if (sales.isEmpty()) EmptyState("No sales recorded", "Create a sale to update stock and record revenue.")
        else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item { Spacer(Modifier.height(8.dp)) }
            items(sales, key = { it.id }) { sale -> SaleRow(sale) { onReceipt(sale) } }
            item { Spacer(Modifier.height(75.dp)) }
        }
        FloatingActionButton(
            onClick = { showSale = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) { Icon(Icons.Default.Add, "New sale") }
    }
    if (showSale) SaleDialog(products, customers, onDismiss = { showSale = false }) { cart, discount, paid, method, customer ->
        vm.saveSale(cart, discount, paid, method, customer)
        showSale = false
    }
}

@Composable
private fun SaleDialog(
    products: List<Product>,
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSave: (List<Pair<Product, Int>>, Long, Long, String, Long?) -> Unit
) {
    val cart = remember { mutableStateListOf<Pair<Product, Int>>() }
    var selected by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var discountText by remember { mutableStateOf("0") }
    var paidText by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }
    var customer by remember { mutableStateOf<Customer?>(null) }
    var productMenu by remember { mutableStateOf(false) }
    var customerMenu by remember { mutableStateOf(false) }
    var methodMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val subtotal = cart.sumOf { it.first.sellingPrice * it.second }
    val discount = parseAmount(discountText) ?: 0L
    val total = (subtotal - discount).coerceAtLeast(0)
    val paid = if (paidText.isBlank()) total else parseAmount(paidText) ?: -1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New sale") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(440.dp)) {
                item {
                    OutlinedButton(onClick = { productMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selected?.name ?: "Choose a product", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ExpandMore, null)
                    }
                    DropdownMenu(productMenu, { productMenu = false }) {
                        products.filter { it.quantity > 0 }.forEach { product ->
                            DropdownMenuItem(
                                text = { Text("${product.name} · ${product.quantity} available") },
                                onClick = { selected = product; productMenu = false }
                            )
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormField("Qty", quantity, { quantity = it }, Modifier.weight(1f), true)
                        Button(onClick = {
                            val product = selected
                            val qty = quantity.toIntOrNull()
                            when {
                                product == null -> error = "Choose a product."
                                qty == null || qty <= 0 -> error = "Enter a valid quantity."
                                qty + (cart.firstOrNull { it.first.id == product.id }?.second ?: 0) > product.quantity ->
                                    error = "Quantity exceeds available stock."
                                else -> {
                                    val index = cart.indexOfFirst { it.first.id == product.id }
                                    if (index >= 0) cart[index] = product to (cart[index].second + qty)
                                    else cart.add(product to qty)
                                    error = null
                                }
                            }
                        }) { Text("Add item") }
                    }
                }
                items(cart.toList(), key = { it.first.id }) { (product, qty) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${product.name} × $qty", modifier = Modifier.weight(1f))
                        Text(money(product.sellingPrice * qty), fontWeight = FontWeight.Medium)
                        TextButton(onClick = { cart.removeAll { it.first.id == product.id } }) { Text("Remove") }
                    }
                }
                item { FormField("Discount (₹)", discountText, { discountText = it }, keyboard = true) }
                item { SummaryRow("Total", money(total)) }
                item {
                    OutlinedButton(onClick = { methodMenu = true }) {
                        Text("Payment: $method")
                        Icon(Icons.Default.ExpandMore, null)
                    }
                    DropdownMenu(methodMenu, { methodMenu = false }) {
                        listOf("Cash", "UPI", "Card", "Credit").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { method = option; methodMenu = false })
                        }
                    }
                }
                if (customers.isNotEmpty()) item {
                    OutlinedButton(onClick = { customerMenu = true }) {
                        Text(customer?.name ?: "Walk-in customer")
                        Icon(Icons.Default.ExpandMore, null)
                    }
                    DropdownMenu(customerMenu, { customerMenu = false }) {
                        DropdownMenuItem(text = { Text("Walk-in customer") }, onClick = { customer = null; customerMenu = false })
                        customers.forEach { option ->
                            DropdownMenuItem(text = { Text(option.name) }, onClick = { customer = option; customerMenu = false })
                        }
                    }
                }
                item { FormField("Amount collected (₹)", paidText, { paidText = it }, keyboard = true) }
                item { Text("Leave blank to mark the full amount as paid. Credit sales should use ₹0.", color = Muted, style = MaterialTheme.typography.labelSmall) }
                item { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    cart.isEmpty() -> error = "Add at least one product."
                    parseAmount(discountText) == null || discount > subtotal -> error = "Enter a valid discount no greater than the subtotal."
                    paid < 0 || paid > total -> error = "Collected amount must be between zero and total."
                    else -> onSave(cart.toList(), discount, paid, method, customer?.id)
                }
            }) { Text("Complete sale") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PurchasesScreen(
    modifier: Modifier,
    products: List<Product>,
    suppliers: List<Supplier>,
    purchases: List<Purchase>,
    vm: MainViewModel
) {
    var show by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        if (purchases.isEmpty()) EmptyState("No purchases recorded", "Record stock purchases to update inventory and supplier balances.")
        else LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(purchases, key = { it.id }) { purchase ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(purchase.invoice.ifBlank { "Purchase #${purchase.id}" }, fontWeight = FontWeight.SemiBold)
                        Text("${products.firstOrNull { it.id == purchase.productId }?.name ?: "Product"} · ${purchase.quantity} pcs · ${date(purchase.purchasedAt)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        SummaryRow("Total", money(purchase.totalCost))
                        SummaryRow("Outstanding", money(purchase.totalCost - purchase.paidAmount))
                    }
                }
            }
        }
        FloatingActionButton(onClick = { show = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Default.Add, "Record purchase")
        }
    }
    if (show) PurchaseDialog(products, suppliers, onDismiss = { show = false }) {
        vm.savePurchase(it)
        show = false
    }
}

@Composable
private fun PurchaseDialog(
    products: List<Product>,
    suppliers: List<Supplier>,
    onDismiss: () -> Unit,
    onSave: (Purchase) -> Unit
) {
    var selected by remember { mutableStateOf(products.firstOrNull()) }
    var supplier by remember { mutableStateOf<Supplier?>(null) }
    var quantity by remember { mutableStateOf("") }
    var unitCost by remember { mutableStateOf("") }
    var transport by remember { mutableStateOf("0") }
    var other by remember { mutableStateOf("0") }
    var paid by remember { mutableStateOf("") }
    var invoice by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var supplierMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val qty = quantity.toIntOrNull() ?: 0
    val cost = parseAmount(unitCost) ?: 0
    val landedTotal = qty * cost + (parseAmount(transport) ?: 0) + (parseAmount(other) ?: 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record purchase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selected?.name ?: "Choose product")
                    Icon(Icons.Default.ExpandMore, null)
                }
                DropdownMenu(menu, { menu = false }) {
                    products.forEach { product -> DropdownMenuItem(text = { Text(product.name) }, onClick = { selected = product; menu = false }) }
                }
                if (suppliers.isNotEmpty()) {
                    OutlinedButton(onClick = { supplierMenu = true }) { Text(supplier?.name ?: "Choose supplier") }
                    DropdownMenu(supplierMenu, { supplierMenu = false }) {
                        suppliers.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { supplier = item; supplierMenu = false }) }
                    }
                }
                FormField("Invoice number", invoice, { invoice = it })
                FormField("Pieces", quantity, { quantity = it }, keyboard = true)
                FormField("Cost per piece (₹)", unitCost, { unitCost = it }, keyboard = true)
                FormField("Transport (₹)", transport, { transport = it }, keyboard = true)
                FormField("Other charges (₹)", other, { other = it }, keyboard = true)
                SummaryRow("Landed purchase cost", money(landedTotal))
                FormField("Amount paid (₹)", paid, { paid = it }, keyboard = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val paidAmount = parseAmount(paid)
                when {
                    selected == null -> error = "Add a product before recording a purchase."
                    qty <= 0 -> error = "Enter a positive quantity."
                    parseAmount(unitCost) == null || parseAmount(transport) == null || parseAmount(other) == null || paidAmount == null ->
                        error = "Enter valid purchase amounts."
                    paidAmount !in 0..landedTotal -> error = "Amount paid cannot exceed purchase cost."
                    else -> onSave(Purchase(
                        supplierId = supplier?.id, invoice = invoice.trim(), productId = selected!!.id,
                        quantity = qty, totalCost = landedTotal, paidAmount = paidAmount,
                        transportCost = parseAmount(transport)!!, otherCharges = parseAmount(other)!!
                    ))
                }
            }) { Text("Save purchase") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ExpensesScreen(modifier: Modifier, expenses: List<Expense>, vm: MainViewModel) {
    var show by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        if (expenses.isEmpty()) EmptyState("No expenses recorded", "Add operating costs to see accurate net profit.")
        else LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(expenses, key = { it.id }) { expense ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(expense.category, fontWeight = FontWeight.SemiBold); Text("${expense.description} · ${date(expense.spentAt)}", color = Muted, style = MaterialTheme.typography.bodySmall) }
                        Text(money(expense.amount), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        FloatingActionButton(onClick = { show = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Default.Add, "Add expense")
        }
    }
    if (show) ExpenseDialog(onDismiss = { show = false }) {
        vm.saveExpense(it)
        show = false
    }
}

@Composable
private fun ExpenseDialog(onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    val categories = listOf("Shop rent", "Electricity", "Internet", "Staff salary", "Transport", "Packaging", "Washing and ironing", "Repairs and maintenance", "Marketing", "Miscellaneous")
    var category by remember { mutableStateOf(categories.first()) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var categoryMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { categoryMenu = true }) { Text(category); Icon(Icons.Default.ExpandMore, null) }
                DropdownMenu(categoryMenu, { categoryMenu = false }) {
                    categories.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { category = option; categoryMenu = false }) }
                    DropdownMenuItem(text = { Text("Custom category…") }, onClick = { category = ""; categoryMenu = false })
                }
                if (category.isBlank()) FormField("Custom category", category, { category = it })
                FormField("Description", description, { description = it })
                FormField("Amount (₹)", amount, { amount = it }, keyboard = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = parseAmount(amount)
                if (category.isBlank()) error = "Expense category is required."
                else if (parsed == null || parsed <= 0) error = "Enter an expense amount greater than zero."
                else onSave(Expense(category = category.trim(), description = description.trim(), amount = parsed))
            }) { Text("Save expense") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MoreScreen(modifier: Modifier, navigate: (String) -> Unit) {
    val options = listOf(
        "Purchases" to "Record stock intake and supplier invoices",
        "Expenses" to "Track operating costs",
        "Suppliers" to "Manage supplier contacts",
        "Customers" to "Manage customer contacts",
        "Udhaar" to "Review and collect outstanding balances",
        "Backup" to "Export, restore or download business data",
        "Settings" to "App and data information"
    )
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items(options) { (title, subtitle) ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navigate(title) },
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (title == "Backup") Icons.Default.Backup else Icons.Default.Category, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Icon(Icons.Default.ExpandMore, null, tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun PartyScreen(
    modifier: Modifier,
    title: String,
    rows: List<Pair<String, String>>,
    totalDue: Long,
    onAdd: (String, String, String) -> Unit
) {
    var show by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            MetricCard("Outstanding balance", totalDue, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            if (rows.isEmpty()) EmptyState("No $title yet", "Add business contacts to keep purchasing and sales records organized.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(rows) { (name, phone) ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(15.dp)) {
                            Text(name, fontWeight = FontWeight.SemiBold)
                            if (phone.isNotBlank()) Text(phone, color = Muted)
                        }
                    }
                }
                item { Spacer(Modifier.height(70.dp)) }
            }
        }
        FloatingActionButton(onClick = { show = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Default.Add, "Add $title")
        }
    }
    if (show) PartyDialog(title, { show = false }, onAdd)
}

@Composable
private fun PartyDialog(title: String, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${title.removeSuffix("s").lowercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField("Name", name, { name = it })
                FormField("Phone", phone, { phone = it }, keyboard = true)
                if (title == "Suppliers") FormField("Address", address, { address = it })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) error = "Name is required."
                else onSave(name.trim(), phone.trim(), address.trim())
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DuesScreen(
    modifier: Modifier,
    sales: List<Sale>,
    purchases: List<Purchase>,
    customers: List<Customer>,
    suppliers: List<Supplier>,
    payments: List<Payment>,
    vm: MainViewModel
) {
    var selectedPayment by remember { mutableStateOf<PaymentTarget?>(null) }
    val customerDues = sales.filter { !it.cancelled && it.totalAmount > it.paidAmount }
    val supplierDues = purchases.filter { it.totalCost > it.paidAmount }
    if (customerDues.isEmpty() && supplierDues.isEmpty()) {
        EmptyState("All balances are clear", "Outstanding customer and supplier balances will appear here.")
    } else {
        LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (customerDues.isNotEmpty()) item { Text("CUSTOMER UDHAR", style = MaterialTheme.typography.labelMedium, color = Muted) }
            items(customerDues, key = { "sale-${it.id}" }) { sale ->
                val customerName = customers.firstOrNull { it.id == sale.customerId }?.name ?: "Walk-in customer"
                DueCard(
                    customerName, "Sale #${sale.id}", sale.totalAmount - sale.paidAmount,
                    onPay = { selectedPayment = PaymentTarget("customer", sale.id, customerName) }
                )
            }
            if (supplierDues.isNotEmpty()) item { Text("SUPPLIER BALANCES", style = MaterialTheme.typography.labelMedium, color = Muted) }
            items(supplierDues, key = { "purchase-${it.id}" }) { purchase ->
                val name = suppliers.firstOrNull { it.id == purchase.supplierId }?.name ?: "Supplier"
                DueCard(
                    name, purchase.invoice.ifBlank { "Purchase #${purchase.id}" },
                    purchase.totalCost - purchase.paidAmount,
                    onPay = { selectedPayment = PaymentTarget("supplier", purchase.id, name) }
                )
            }
            if (payments.isNotEmpty()) item {
                Text("PAYMENT HISTORY", style = MaterialTheme.typography.labelMedium, color = Muted)
                payments.take(10).forEach { payment ->
                    SummaryRow("${payment.partyType.replaceFirstChar { it.uppercase() }} · ${date(payment.paidAt)}", money(payment.amount))
                }
            }
        }
    }
    selectedPayment?.let { target ->
        PaymentDialog(target, onDismiss = { selectedPayment = null }) { amount ->
            vm.collectPayment(Payment(partyType = target.type, partyId = target.id, amount = amount))
            selectedPayment = null
        }
    }
}

private data class PaymentTarget(val type: String, val id: Long, val name: String)

@Composable
private fun DueCard(name: String, reference: String, amount: Long, onPay: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(reference, color = Muted, style = MaterialTheme.typography.bodySmall)
                Text("Due ${money(amount)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onPay) { Text(if (reference.startsWith("Purchase")) "Record payment" else "Collect") }
        }
    }
}

@Composable
private fun PaymentDialog(target: PaymentTarget, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (target.type == "customer") "Collect payment" else "Pay supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${target.name} · ${target.type} record #${target.id}")
                FormField("Amount (₹)", amount, { amount = it }, keyboard = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = parseAmount(amount)
                if (parsed == null || parsed <= 0) error = "Enter a payment greater than zero."
                else onSave(parsed)
            }) { Text("Save payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BackupScreen(modifier: Modifier, onExport: () -> Unit, onRestore: () -> Unit, onCsv: () -> Unit) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Keep a safe copy of your shop records.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("JSON includes inventory, sales, sale items, purchases, expenses, contacts and payment history. Restore replaces local records only after confirmation.", color = Muted)
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Backup, null); Spacer(Modifier.width(8.dp)); Text("Create JSON backup") }
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("Restore from JSON backup") }
        OutlinedButton(onClick = onCsv, modifier = Modifier.fillMaxWidth()) { Text("Export records as CSV") }
        Text("Backup files are saved to a location you choose and are not uploaded automatically.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("App settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Local business data", fontWeight = FontWeight.SemiBold)
                Text("Records are stored in the app's local Room database. Create a backup before moving to another device.", color = Muted)
            }
        }
        Text("Thrift Manager · Version 1.0.0", color = Muted)
    }
}

@Composable
private fun ReportsScreen(
    modifier: Modifier,
    sales: List<Sale>,
    expenses: List<Expense>,
    purchases: List<Purchase>,
    inventoryValue: Long,
    customerDue: Long,
    supplierDue: Long,
    onCsv: () -> Unit,
    onPdf: () -> Unit
) {
    val days = (6 downTo 0).map { offset ->
        val start = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -offset)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val from = start.timeInMillis
        val until = from + 86_400_000L
        dateShort(from) to Pair(
            sales.filter { !it.cancelled && it.soldAt in from until until }.sumOf { it.totalAmount },
            expenses.filter { it.spentAt in from until until }.sumOf { it.amount }
        )
    }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Last 7 days · sales & expenses", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Bars(days.map { it.first to it.second.first }, Color(0xFF596671))
                    Spacer(Modifier.height(6.dp))
                    Text("Sales", color = Muted, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(12.dp))
                    Bars(days.map { it.first to it.second.second }, Color(0xFF9BA6AE))
                    Spacer(Modifier.height(6.dp))
                    Text("Expenses", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("All-time recorded totals", fontWeight = FontWeight.SemiBold)
                    SummaryRow("Sales revenue", money(sales.filterNot { it.cancelled }.sumOf { it.totalAmount }))
                    SummaryRow("Sales collected", money(sales.filterNot { it.cancelled }.sumOf { it.paidAmount }))
                    SummaryRow("Operating expenses", money(expenses.sumOf { it.amount }))
                    SummaryRow("Purchase costs", money(purchases.sumOf { it.totalCost }))
                    SummaryRow("Inventory at cost", money(inventoryValue))
                    SummaryRow("Customer outstanding", money(customerDue))
                    SummaryRow("Supplier outstanding", money(supplierDue))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCsv, modifier = Modifier.weight(1f)) { Text("Export CSV") }
                Button(onClick = onPdf, modifier = Modifier.weight(1f)) { Text("Export PDF") }
            }
        }
    }
}

@Composable
private fun Bars(values: List<Pair<String, Long>>, color: Color) {
    val max = (values.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L).toFloat()
    Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
        values.forEach { (label, amount) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Canvas(Modifier.fillMaxWidth().height(70.dp)) {
                    val barHeight = (amount.toFloat() / max * size.height).coerceAtLeast(if (amount > 0) 3f else 0f)
                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * .18f, size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(size.width * .64f, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                    )
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboard: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (keyboard) KeyboardType.Decimal else KeyboardType.Text)
    )
}

private fun parseAmount(value: String): Long? {
    val amount = value.trim().toBigDecimalOrNull() ?: return null
    if (amount.scale() > 2 || amount < java.math.BigDecimal.ZERO) return null
    return runCatching { amount.movePointRight(2).longValueExact() }.getOrNull()
}

private fun displayAmount(paise: Long): String = "%.2f".format(java.util.Locale.US, paise / 100.0)
private fun money(paise: Long): String = "₹${displayAmount(paise)}"
private fun date(time: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(time))
private fun dateShort(time: Long): String = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(Date(time))
private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
