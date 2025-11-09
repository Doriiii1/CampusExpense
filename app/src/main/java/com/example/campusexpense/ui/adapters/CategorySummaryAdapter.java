package com.example.campusexpense.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campusexpense.R;
import com.example.campusexpense.reports.ReportGenerator;
import java.util.List;
import java.util.Locale;

/**
 * CategorySummaryAdapter - RecyclerView adapter for category summaries
 * Phase 4: Overview and Reports
 *
 * Features:
 * - Display category name, total, and percentage
 * - Color-coded progress bars
 * - Budget status indicators
 * - Click listener for detail navigation
 *
 * Accessibility: contentDescription on actionable items
 */
public class CategorySummaryAdapter extends RecyclerView.Adapter<CategorySummaryAdapter.CategoryViewHolder> {

    private final List<ReportGenerator.CategorySummary> summaries;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(ReportGenerator.CategorySummary summary);
    }

    public CategorySummaryAdapter(List<ReportGenerator.CategorySummary> summaries, OnCategoryClickListener listener) {
        this.summaries = summaries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_summary, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        ReportGenerator.CategorySummary summary = summaries.get(position);
        holder.bind(summary, listener);
    }

    @Override
    public int getItemCount() {
        return summaries.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView textCategory;
        private final TextView textTotal;
        private final TextView textPercentage;
        private final ProgressBar progressBar;
        private final View budgetIndicator;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategory = itemView.findViewById(R.id.textSummaryCategory);
            textTotal = itemView.findViewById(R.id.textSummaryTotal);
            textPercentage = itemView.findViewById(R.id.textSummaryPercentage);
            progressBar = itemView.findViewById(R.id.progressSummary);
            budgetIndicator = itemView.findViewById(R.id.budgetIndicator);
        }

        void bind(ReportGenerator.CategorySummary summary, OnCategoryClickListener listener) {
            textCategory.setText(summary.category);
            textTotal.setText(String.format(Locale.getDefault(), "$%.2f", summary.total));
            textPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", summary.percentage));

            // Progress bar (percentage of total spending)
            progressBar.setProgress((int) summary.percentage);

            // Budget indicator
            if (summary.hasBudget) {
                budgetIndicator.setVisibility(View.VISIBLE);

                // Color code based on budget status
                int colorResId;
                if (summary.budgetProgress >= 100) {
                    colorResId = R.color.danger_red;
                } else if (summary.budgetProgress >= 80) {
                    colorResId = R.color.warning_orange;
                } else {
                    colorResId = R.color.success_green;
                }

                int color = itemView.getContext().getColor(colorResId);
                budgetIndicator.setBackgroundColor(color);

                // Set content description for accessibility
                String budgetStatus = summary.budgetProgress >= 100 ? "over budget" :
                        summary.budgetProgress >= 80 ? "near budget limit" : "within budget";
                budgetIndicator.setContentDescription(summary.category + " is " + budgetStatus);
            } else {
                budgetIndicator.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> listener.onCategoryClick(summary));
        }
    }
}