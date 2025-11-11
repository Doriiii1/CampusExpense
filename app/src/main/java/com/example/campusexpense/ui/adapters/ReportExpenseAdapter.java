package com.example.campusexpense.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campusexpense.R;
import com.example.campusexpense.model.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ReportExpenseAdapter extends RecyclerView.Adapter<ReportExpenseAdapter.ViewHolder> {

    private List<Expense> expenses;
    private String sortBy;  // "date" or "amount"

    public ReportExpenseAdapter(List<Expense> expenses, String sortBy) {
        this.expenses = expenses != null ? expenses : new ArrayList<>();
        this.sortBy = sortBy;
        sortList();  // Initial sort
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.textCategory.setText(expense.getCategory());
        holder.textDescription.setText(expense.getDescription());
        holder.textAmount.setText(String.format("$%.2f", expense.getAmount()));
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.textDate.setText(sdf.format(expense.getDate()));  // Format as needed
        // Thêm % budget nếu cần: holder.textBudgetPercent.setText(calculatePercent(expense));
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public void updateList(List<Expense> newExpenses, String newSortBy) {
        this.expenses.clear();
        this.expenses.addAll(newExpenses != null ? newExpenses : new ArrayList<>());
        this.sortBy = newSortBy;
        sortList();
        notifyDataSetChanged();  // Hoặc dùng DiffUtil cho perf
    }

    private void sortList() {
        if ("amount".equals(sortBy)) {
            Collections.sort(expenses, Comparator.comparingDouble(Expense::getAmount).reversed());
        } else {  // default "date"
            Collections.sort(expenses, Comparator.comparing(Expense::getDate));  // Assume date as String parsable
        }
    }

    // ViewHolder class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textCategory, textDescription, textAmount, textDate;  // Add textBudgetPercent if needed

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategory = itemView.findViewById(R.id.textCategory);
            textDescription = itemView.findViewById(R.id.textDescription);
            textAmount = itemView.findViewById(R.id.textAmount);
            textDate = itemView.findViewById(R.id.textDate);
            // textBudgetPercent = itemView.findViewById(R.id.textBudgetPercent);
        }
    }
}