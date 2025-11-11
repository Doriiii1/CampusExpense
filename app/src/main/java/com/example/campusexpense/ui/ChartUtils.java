package com.example.campusexpense.ui;

import android.graphics.Color;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ChartUtils - Helper methods for MPAndroidChart
 * Phase 4: Overview and Reports
 *
 * Features:
 * - PieChart configuration for category distribution
 * - Color schemes and formatting
 * - Accessibility support
 *
 * Library: MPAndroidChart 3.1.0
 */
public class ChartUtils {

    /**
     * Configure and populate PieChart with category data
     * @param pieChart PieChart view
     * @param categoryTotals Map of category to total amount
     */
    public static void setupPieChart(PieChart pieChart, Map<String, Double> categoryTotals) {
        if (categoryTotals == null || categoryTotals.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("No data available");
            pieChart.invalidate();
            return;
        }

        // Create entries
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        // Create dataset
        PieDataSet dataSet = new PieDataSet(entries, "Expenses by Category");
        dataSet.setColors(getChartColors());
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        // Create data
        PieData data = new PieData(dataSet);

        // Configure chart
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setHoleRadius(58f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Spending\nDistribution");
        pieChart.setCenterTextSize(14f);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        // Legend
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextSize(11f);
        pieChart.getLegend().setWordWrapEnabled(true);

        // Animation
        pieChart.animateY(1000);

        // Refresh
        pieChart.invalidate();

        // Accessibility
        pieChart.setContentDescription("Pie chart showing expense distribution by category");
    }

    /**
     * Get color palette for charts
     * @return List of colors
     */
    private static List<Integer> getChartColors() {
        List<Integer> colors = new ArrayList<>();

        // Material Design colors
        colors.add(Color.rgb(103, 80, 164)); // Deep Purple
        colors.add(Color.rgb(244, 67, 54));  // Red
        colors.add(Color.rgb(33, 150, 243)); // Blue
        colors.add(Color.rgb(76, 175, 80));  // Green
        colors.add(Color.rgb(255, 152, 0));  // Orange
        colors.add(Color.rgb(156, 39, 176)); // Purple
        colors.add(Color.rgb(0, 188, 212));  // Cyan
        colors.add(Color.rgb(255, 235, 59)); // Yellow

        // Add more colors from ColorTemplate if needed
        for (int color : ColorTemplate.MATERIAL_COLORS) {
            colors.add(color);
        }

        return colors;
    }
}