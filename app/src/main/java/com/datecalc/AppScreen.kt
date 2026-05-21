package com.datecalc

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datecalc.billing.BillingFactory
import com.datecalc.billing.PaywallScreen
import com.datecalc.billing.SubscriptionManager
import com.datecalc.logic.DateCalculator
import com.datecalc.logic.DateCalcResult
import com.datecalc.ui.components.DateCard
import com.datecalc.ui.components.ResultBox
import com.datecalc.ui.theme.DateCalcTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(activity: Activity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val billingClient = remember { BillingFactory.create(context) }

    var darkTheme by remember { mutableStateOf(false) }
    var hasAccess by remember { mutableStateOf(true) }
    var isSubscribed by remember { mutableStateOf(false) }
    var trialDaysLeft by remember { mutableIntStateOf(7) }
    var showPaywall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        billingClient.initialize()
        isSubscribed = SubscriptionManager.isSubscribed(context).first()
        if (!isSubscribed) {
            val trialStarted = SubscriptionManager.trialStarted(context).first()
            if (!trialStarted) SubscriptionManager.startTrial(context)
            val storePurchased = billingClient.checkPurchases()
            if (storePurchased) {
                SubscriptionManager.setSubscribed(context, true)
                isSubscribed = true
                hasAccess = true
            } else {
                hasAccess = SubscriptionManager.hasAccess(context).first()
                trialDaysLeft = SubscriptionManager.trialDaysRemaining(context).first()
                if (!hasAccess) showPaywall = true
            }
        } else {
            hasAccess = true
        }
    }

    DateCalcTheme(darkTheme = darkTheme) {
        if (showPaywall && !hasAccess) {
            PaywallScreen(
                trialDaysRemaining = trialDaysLeft,
                onSubscribe = {
                    scope.launch {
                        try {
                            val success = billingClient.purchase(activity)
                            if (success) {
                                SubscriptionManager.setSubscribed(context, true)
                                isSubscribed = true; hasAccess = true; showPaywall = false
                            }
                        } catch (_: Exception) {}
                    }
                },
                onRestore = {
                    scope.launch {
                        try {
                            if (billingClient.checkPurchases()) {
                                SubscriptionManager.setSubscribed(context, true)
                                isSubscribed = true; hasAccess = true; showPaywall = false
                            }
                        } catch (_: Exception) {}
                    }
                },
                onNotNow = { showPaywall = false; hasAccess = true }
            )
        } else {
            MainContent(
                darkTheme = darkTheme,
                onToggleTheme = { darkTheme = !darkTheme },
                isSubscribed = isSubscribed,
                trialDaysLeft = trialDaysLeft,
                onShowPaywall = { showPaywall = true }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    isSubscribed: Boolean,
    trialDaysLeft: Int,
    onShowPaywall: () -> Unit
) {
    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    val todayMonth = cal.get(Calendar.MONTH)
    val todayYear = cal.get(Calendar.YEAR)

    var startDay by remember { mutableIntStateOf(todayDay) }
    var startMonth by remember { mutableIntStateOf(todayMonth) }
    var startYear by remember { mutableIntStateOf(todayYear) }

    var endDay by remember { mutableIntStateOf(todayDay) }
    var endMonth by remember { mutableIntStateOf(todayMonth) }
    var endYear by remember { mutableIntStateOf(todayYear) }

    var includeStart by remember { mutableStateOf(true) }
    var includeEnd by remember { mutableStateOf(true) }

    var result by remember { mutableStateOf(DateCalcResult()) }
    var error by remember { mutableStateOf("") }

    val startMaxDay = remember(startYear, startMonth) { DateCalculator.daysInMonth(startYear, startMonth) }
    val endMaxDay = remember(endYear, endMonth) { DateCalculator.daysInMonth(endYear, endMonth) }

    val safeStartDay = startDay.coerceAtMost(startMaxDay)
    val safeEndDay = endDay.coerceAtMost(endMaxDay)

    val startDays = remember(startYear, startMonth) {
        DateCalculator.validDaysForMonth(startYear, startMonth).map { it.toString() }
    }
    val endDays = remember(endYear, endMonth) {
        DateCalculator.validDaysForMonth(endYear, endMonth).map { it.toString() }
    }

    LaunchedEffect(safeStartDay, startMonth, startYear, safeEndDay, endMonth, endYear, includeStart, includeEnd) {
        if (startDay != safeStartDay) startDay = safeStartDay
        if (endDay != safeEndDay) endDay = safeEndDay
        result = DateCalculator.calculate(safeStartDay, startMonth, startYear, safeEndDay, endMonth, endYear, includeStart, includeEnd)
        error = result.error
    }

    fun resetFields() {
        startDay = todayDay; startMonth = todayMonth; startYear = todayYear
        endDay = todayDay; endMonth = todayMonth; endYear = todayYear
        includeStart = true; includeEnd = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Дата-калькулятор", fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                },
                actions = {
                    if (!isSubscribed && trialDaysLeft > 0) {
                        Text("$trialDaysLeft дн.", fontSize = 12.sp, color = Color(0xFF007AFF),
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 8.dp, top = 18.dp))
                    }
                    if (!isSubscribed) {
                        TextButton(onClick = onShowPaywall) { Text("Премиум", fontSize = 13.sp, color = Color(0xFF007AFF)) }
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (darkTheme) "Светлая тема" else "Тёмная тема",
                            tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { resetFields() }) {
                        Icon(Icons.Filled.Refresh, "Сбросить", tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DateCard(
                title = "Дата начала", icon = "calendar", accentColor = Color(0xFF007AFF),
                selectedDay = safeStartDay, selectedMonth = startMonth, selectedYear = startYear,
                availableDays = startDays, toggleTitle = "Учитывать начальную дату", toggleChecked = includeStart,
                onDateChange = { d, m, y -> startDay = d; startMonth = m; startYear = y },
                onToggleChange = { includeStart = it }
            )

            DateCard(
                title = "Дата окончания", icon = "clock", accentColor = Color(0xFFFF9500),
                selectedDay = safeEndDay, selectedMonth = endMonth, selectedYear = endYear,
                availableDays = endDays, toggleTitle = "Учитывать конечную дату", toggleChecked = includeEnd,
                onDateChange = { d, m, y -> endDay = d; endMonth = m; endYear = y },
                onToggleChange = { includeEnd = it }
            )

            if (error.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(error, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                }
            }

            ResultBox(
                result = result,
                description = DateCalculator.resultDescription(includeStart, includeEnd)
            )
        }
    }
}
