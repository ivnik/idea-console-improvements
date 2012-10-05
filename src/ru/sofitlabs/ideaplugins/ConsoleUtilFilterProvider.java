package ru.sofitlabs.ideaplugins;

import com.intellij.execution.filters.*;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
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
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleUtilFilterProvider implements ConsoleFilterProvider {
    @NotNull
    @Override
    public Filter[] getDefaultFilters(@NotNull final Project project) {
        final Filter fileErorrFilter = new FileErrorFilter(project);

        final Filter classnameFilter = new Filter() {
//            private final Pattern classNamePattern = Pattern.compile("([\\p{Alpha}\\.\\$]+)(?::?\\[?(\\d+)\\]?)?");
            private final Pattern classNamePattern = Pattern.compile("([\\p{Alpha}\\.\\$]+)(?:[:\\[\\]\\(\\)\\s]*(\\d+))?(?:[:\\[\\]\\(\\)\\s]*(\\d+))?[\\]\\)]?");

            @Override
            public Result applyFilter(final String line, final int entireLength) {
                final int startPoint = entireLength - line.length();

                Matcher matcher = classNamePattern.matcher(line);

                while (matcher.find()) {
                    final String name = matcher.group(1).replace('$', '.');
                    final String gr2 = matcher.group(2);
                    final String gr3 = matcher.group(3);

                    final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
                    final PsiClass clazz = JavaPsiFacade.getInstance(project).findClass(name, scope);

                    if (clazz != null) {
                        return new Result(startPoint + matcher.start(), startPoint + matcher.end(), new HyperlinkInfo() {
                            @Override
                            public void navigate(final Project project) {
                                ApplicationManager.getApplication().runReadAction(new Runnable() {
                                    public void run() {
                                        final VirtualFile file = clazz.getContainingFile().getVirtualFile();
                                        if (file.isValid()) {
                                            int line = gr2 != null ? Integer.parseInt(gr2) : 0;
                                            int column = gr3 != null ? Integer.parseInt(gr3) : 0;

                                            FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, file, line - 1, column - 1), true);
                                        }
                                    }
                                });

                            }
                        });
                    }
                }

                return null;
            }
        };

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
                fileErorrFilter,
                colorifyFilter,
                classnameFilter
        };
    }

    private static class FileErrorFilter implements Filter {
        private final Pattern checkstyleErrorPattern;
        private final Project project;

        public FileErrorFilter(Project project) {
            this.project = project;
            checkstyleErrorPattern = Pattern.compile("^(?:\\[(?:ERROR)\\]\\s*)?((?:\\p{Alpha}\\:)?[0-9 a-z_A-Z\\-\\\\./]+):(\\d+)(?::(\\d+))?");
        }

        @Override
        public Result applyFilter(final String line, final int entireLength) {
            final int startPoint = entireLength - line.length();

            Matcher matcher = checkstyleErrorPattern.matcher(line);
            if (matcher.find()) {
                final int group1Start = line.indexOf(matcher.group(1));
                final int group1End = group1Start + matcher.group(1).length();


                final String path = matcher.group(1).replace(File.separatorChar, '/');
                final int documentLine = Integer.parseInt(matcher.group(2));
                final String gr3 = matcher.group(3);

                final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);

                if (file != null) {
                    if (gr3 != null) {
                        final int documentColumn = Integer.parseInt(gr3);

                        return new Result(startPoint + group1Start, startPoint + group1End, new OpenFileHyperlinkInfo(project, file, documentLine - 1, documentColumn - 1));
                    } else {
                        return new Result(startPoint + group1Start, startPoint + group1End, new OpenFileHyperlinkInfo(project, file, documentLine - 1));
                    }
                } else {
                    return new Result(startPoint, entireLength, null, new TextAttributes());
                }
            } else {
                return null;
            }
        }
    }
}
