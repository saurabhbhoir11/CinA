package ir.ashkanabd.cina.view.FileBrowser;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.appcompat.widget.AppCompatImageView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.beardedhen.androidbootstrap.BootstrapButton;
import com.rey.material.widget.EditText;
import com.unnamed.b.atv.model.TreeNode;
import ir.ashkanabd.cina.R;
import ir.ashkanabd.cina.project.ProjectManager;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FileBrowserListeners {

    static int NONE = 0;
    static int CREATE_FILE = 1;
    static int CREATE_FOLDER = 2;

    private View preClickedView;
    private FileBrowserDialog fileBrowserDialog;
    private MaterialDialog fileNameDialog;
    private EditText fileNameEditText;
    private boolean validFileName = false;
    private int createMode = NONE;

    FileBrowserListeners(FileBrowserDialog browserDialog) {
        this.fileBrowserDialog = browserDialog;
        setupFileNameDialog();
    }

    private void setupFileNameDialog() {
        this.fileNameDialog = new MaterialDialog(fileBrowserDialog.getActivity());
        this.fileNameDialog.setContentView(R.layout.new_file_name_layout);
        this.fileNameDialog.setCancelable(true);
        BootstrapButton createButton = fileNameDialog.findViewById(R.id.create_button_new_file_name);
        BootstrapButton cancelButton = fileNameDialog.findViewById(R.id.cancel_button_new_file_name);
        fileNameEditText = fileNameDialog.findViewById(R.id.edit_new_file_name);
        createButton.setOnClickListener(this::onFileCreateButtonClick);
        cancelButton.setOnClickListener(this::onFileCancelButtonListener);
        fileNameEditText.addTextChangedListener(new FileNameTextWatcher());
        this.fileNameDialog.setOnDismissListener(this::onCreateNewFileDialogDismiss);
    }

    /**
     * Call when On a {@link TreeNode} clicked
     *
     * @param node  {@link TreeNode} clicked tree node
     * @param value {@link File} the file that {@link TreeNode} points to it
     */
    void onNodeClick(TreeNode node, Object value) {
        FileView fileView = (FileView) node.getViewHolder();
        if (preClickedView != null) {
            preClickedView.setBackgroundColor(Color.WHITE);
        }
        preClickedView = fileView.getView().findViewById(R.id.main_layout_file_layout);
        preClickedView.setBackgroundColor(Color.parseColor("#FFFFFAD6"));
    }

    void onFileStatusClick(View view) {
        AppCompatImageView imageView = (AppCompatImageView) view;
        TreeNode node = (TreeNode) ((View) imageView.getParent()).getTag();
        if (imageView.getVisibility() == View.VISIBLE) {
            boolean isOpen = "open".equals(imageView.getTag());
            if (isOpen) {
                imageView.setImageDrawable(fileBrowserDialog.getActivity().getResources().getDrawable(R.drawable.close_folder_icon));
                imageView.setTag("close");
                fileBrowserDialog.getAndroidTreeView().toggleNode(node);
            } else {
                imageView.setImageDrawable(fileBrowserDialog.getActivity().getResources().getDrawable(R.drawable.open_folder_icon));
                imageView.setTag("open");
                fileBrowserDialog.getAndroidTreeView().toggleNode(node);
            }
        }
    }

    private void onFileCreateButtonClick(View view) {
        try {
            if (!validFileName) return;
            String fileName = this.fileNameEditText.getText().toString();
            File file = fileBrowserDialog.getFile(preClickedView);
            File newFile = new File(file, fileName);
            if (this.createMode == CREATE_FILE)
                newFile.createNewFile();
            if (this.createMode == CREATE_FOLDER)
                newFile.mkdirs();
            fileBrowserDialog.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.fileNameDialog.dismiss();
        fileBrowserDialog.getBrowserDialog().dismiss();
        fileBrowserDialog.load();
        fileBrowserDialog.getBrowserDialog().show();
    }

    private void onFileCancelButtonListener(View view) {
        this.fileNameDialog.dismiss();
    }

    void onNewFileButtonClick(View view) {
        if (preClickedView == null) return;
        File file = fileBrowserDialog.getFile(preClickedView);
        if (!file.isDirectory()) return;
        createMode = CREATE_FILE;
        fileNameEditText.setHint("Enter file name");
        this.fileNameDialog.show();
    }

    void onNewDirButtonClick(View view) {
        if (preClickedView == null) return;
        File file = fileBrowserDialog.getFile(preClickedView);
        if (!file.isDirectory()) return;
        createMode = CREATE_FOLDER;
        fileNameEditText.setHint("Enter folder name");
        this.fileNameDialog.show();
    }

    void onDeleteButtonClick(View view) {
        if (preClickedView == null) return;
        File file = fileBrowserDialog.getFile(preClickedView);
        if (fileBrowserDialog.getSelectedProject().getDir().equals(file.getAbsolutePath()))
            return;
        ProjectManager.remove(file);
        fileBrowserDialog.getBrowserDialog().dismiss();
        fileBrowserDialog.load();
        fileBrowserDialog.getBrowserDialog().show();
    }

    void onOpenButtonClick(View view) {
        if (preClickedView == null) return;
        File file = fileBrowserDialog.getFile(preClickedView);
        if (file.isDirectory()) return;
        try {
            fileBrowserDialog.getActivity().getEditor().setText(fileBrowserDialog.getActivity().readTargetFile(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileBrowserDialog.getBrowserDialog().dismiss();
    }

    void onCancelButtonClick(View view) {
        fileBrowserDialog.getBrowserDialog().dismiss();
        ((Dialog) fileBrowserDialog.getBrowserDialog()).dismiss();
    }

    void onFileBrowserDialogDismiss(DialogInterface dialogInterface) {
        if (preClickedView != null)
            preClickedView.setBackgroundColor(Color.WHITE);
        preClickedView = null;
        createMode = NONE;
    }

    public void onCreateNewFileDialogDismiss(DialogInterface dialog) {
        fileNameEditText.setError("");
        fileNameEditText.setText("");
        fileNameEditText.setHelper("");
    }

    protected class FileNameTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String str = editable.toString();
            int len = str.length();
            Object[] objs = checkFileName(str);
            if (!(boolean) objs[0]) {
                FileBrowserListeners.this.fileNameEditText.setHelper("Error: \"" + objs[1] + "\"");
                FileBrowserListeners.this.validFileName = false;
                return;
            }
            if (len > 10) {
                FileBrowserListeners.this.fileNameEditText.setHelper("too long name = " + len + " characters");
                FileBrowserListeners.this.validFileName = false;
                return;
            }
            FileBrowserListeners.this.fileNameEditText.setHelper("");
            FileBrowserListeners.this.validFileName = true;
        }

        private Object[] checkFileName(String str) {
            Pattern fileNamePattern = null;
            if (FileBrowserListeners.this.createMode == FileBrowserListeners.CREATE_FILE)
                fileNamePattern = Pattern.compile("[a-zA-Z0-9_]+\\.[a-zA-Z]+");
            if (FileBrowserListeners.this.createMode == FileBrowserListeners.CREATE_FOLDER)
                fileNamePattern = Pattern.compile("[a-zA-Z0-9_]+");
            if (fileNamePattern == null) return new Object[]{true, "Unknown error"};
            Matcher fileNameMatcher = fileNamePattern.matcher(str);
            if (!fileNameMatcher.matches()) {
                return new Object[]{false, "Invalid name"};
            }
            if (FileBrowserListeners.this.createMode == FileBrowserListeners.CREATE_FILE)
                if (!FileBrowserListeners.this.fileBrowserDialog.getFileBrowser().checkFileName(str))
                    return new Object[]{false, "Invalid file format"};
            return new Object[]{true};
        }
    }
}