package ru.sofitlabs.ideaplugins;

import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorifyConsoleFilterProvider implements ConsoleFilterProvider {
    @NotNull
    @Override
    public Filter[] getDefaultFilters(@NotNull final Project project) {
        final Filter colorifyFilter = new Filter() {
            private final TextAttributes warnTextAttr = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(ConsoleViewContentType.LOG_WARNING_OUTPUT_KEY);
            private final TextAttributes errorTextAttr = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(ConsoleViewContentType.LOG_ERROR_OUTPUT_KEY);
            private final TextAttributes debugTextAttr = new TextAttributes(Color.GRAY, errorTextAttr.getBackgroundColor(), null, null, Font.PLAIN);

            @Override
            public Result applyFilter(final String line, final int entireLength) {
                final int startPoint = entireLength - line.length();

                if (line.contains("WARN")) {
                    return new Result(startPoint, entireLength, null, warnTextAttr);
                } else if (line.contains("ERROR")) {
                    return new Result(startPoint, entireLength, null, errorTextAttr);
                } else if (line.contains("DEBUG")) {
                    return new Result(startPoint, entireLength, null, debugTextAttr);
                } else {
                    return null;
                }
            }
        };

        return new Filter[]{
                colorifyFilter,
        };
    }
}
