package org.campusfix.android.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import org.campusfix.android.R;
import org.campusfix.android.viewmodel.CampusFixViewModel;
import org.campusfix.android.viewmodel.CampusFixViewModel.CategoriesState;
import org.campusfix.android.viewmodel.CampusFixViewModel.CategoryDisplay;

final class CategoryPanel {
    private static final String DRAFT_ID = "categoryDraftId";
    private static final String DRAFT_NAME = "categoryDraftName";
    private static final String RENDERED_SAVE_COUNT = "categoryRenderedSaveCount";

    private final MainActivity activity;
    private final CampusFixViewModel viewModel;
    private final View panel;
    private final ScrollView screen;
    private final LinearLayout list;
    private final TextView heading;
    private final TextView status;
    private final EditText name;
    private final Button save;
    private final Button discard;
    private List<CategoryDisplay> displayed = List.of();
    private long editingId;
    private long renderedSaveCount;

    CategoryPanel(MainActivity activity, CampusFixViewModel viewModel) {
        this.activity = activity;
        this.viewModel = viewModel;
        screen = activity.findViewById(R.id.screen);
        panel = activity.findViewById(R.id.category_panel);
        list = panel.findViewById(R.id.category_list);
        heading = panel.findViewById(R.id.category_heading);
        status = panel.findViewById(R.id.category_status);
        name = panel.findViewById(R.id.category_name);
        save = panel.findViewById(R.id.save_category);
        discard = panel.findViewById(R.id.discard_category);

        save.setOnClickListener(view -> {
            activity.hideKeyboard(name);
            viewModel.saveCategory(editingId, name.getText().toString());
        });
        discard.setOnClickListener(view -> {
            activity.hideKeyboard(name);
            clearDraft();
            renderDraft();
        });
    }

    void render(CategoriesState state) {
        if (state == null) {
            panel.setVisibility(View.GONE);
            clearDraft();
            renderedSaveCount = 0;
            renderRows(List.of(), false);
            status.setText("");
            return;
        }

        panel.setVisibility(View.VISIBLE);
        if (renderedSaveCount != state.savedNameCount()) {
            renderedSaveCount = state.savedNameCount();
            clearDraft();
        }
        renderDraft();
        if (state.validationMessage() == null) {
            status.setText(state.message());
        } else {
            status.setText(state.validationMessage());
        }
        boolean ready = !state.busy();
        name.setEnabled(ready);
        save.setEnabled(ready);
        discard.setEnabled(ready);
        renderRows(state.items(), ready);
    }

    private void renderRows(List<CategoryDisplay> rows, boolean ready) {
        if (!displayed.equals(rows)) {
            displayed = rows;
            list.removeAllViews();
            for (var category : rows) {
                list.addView(createRow(category));
            }
        }
        for (int index = 0; index < list.getChildCount(); index++) {
            var row = list.getChildAt(index);
            row.findViewById(R.id.rename_category).setEnabled(ready);
            row.findViewById(R.id.toggle_category).setEnabled(ready);
        }
    }

    private View createRow(CategoryDisplay category) {
        var row = activity.getLayoutInflater().inflate(R.layout.category_row, list, false);
        TextView rowName = row.findViewById(R.id.category_row_name);
        TextView rowStatus = row.findViewById(R.id.category_row_status);
        Button rename = row.findViewById(R.id.rename_category);
        Button toggle = row.findViewById(R.id.toggle_category);
        rowName.setText(category.name());
        rowStatus.setText(category.active() ? R.string.category_active : R.string.category_inactive);
        toggle.setText(category.active() ? R.string.deactivate_category : R.string.reactivate_category);
        toggle.setOnClickListener(view -> viewModel.setCategoryActive(category.id(), !category.active()));
        rename.setOnClickListener(view -> {
            editingId = category.id();
            name.setText(category.name());
            renderDraft();
            name.requestFocus();
            name.selectAll();
            screen.smoothScrollTo(0, 0);
        });
        return row;
    }

    private void renderDraft() {
        boolean editing = editingId != 0;
        heading.setText(editing ? R.string.rename_category_heading : R.string.new_category);
        save.setText(editing ? R.string.save_category_changes : R.string.add_category);
        discard.setVisibility(editing ? View.VISIBLE : View.GONE);
    }

    private void clearDraft() {
        editingId = 0;
        name.getText().clear();
    }

    void restoreDraft(Bundle state) {
        if (state != null) {
            editingId = state.getLong(DRAFT_ID);
            name.setText(state.getString(DRAFT_NAME, ""));
            renderedSaveCount = state.getLong(RENDERED_SAVE_COUNT);
        }
    }

    void saveDraft(Bundle state) {
        state.putLong(DRAFT_ID, editingId);
        state.putString(DRAFT_NAME, name.getText().toString());
        state.putLong(RENDERED_SAVE_COUNT, renderedSaveCount);
    }
}
