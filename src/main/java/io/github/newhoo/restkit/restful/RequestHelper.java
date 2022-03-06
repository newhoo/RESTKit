package io.github.newhoo.restkit.restful;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import io.github.newhoo.restkit.common.PsiRestItem;
import io.github.newhoo.restkit.common.RestItem;
import io.github.newhoo.restkit.config.CommonSetting;
import io.github.newhoo.restkit.config.CommonSettingComponent;
import io.github.newhoo.restkit.restful.ep.RestfulResolverProvider;
import io.github.newhoo.restkit.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 服务相关工具类
 *
 * @author huzunrong
 */
public class RequestHelper {

    public static List<RequestResolver> getAllRequestResolvers(@NotNull Project project) {
        return RestfulResolverProvider.EP_NAME.getExtensionList()
                                              .stream()
                                              .filter(Objects::nonNull)
                                              .map(restfulResolverProvider -> restfulResolverProvider.createRequestResolver(project))
                                              .filter(Objects::nonNull)
                                              .sorted(Comparator.comparing(RequestResolver::getScanType))
                                              .collect(Collectors.toList());
    }

    public static List<RequestResolver> getEnabledRequestResolvers(@NotNull Project project) {
        CommonSetting commonSetting = CommonSettingComponent.getInstance(project).getState();
        Set<String> enabledWebFrameworks = commonSetting.getEnabledWebFrameworks();
        return RestfulResolverProvider.EP_NAME.getExtensionList()
                                              .stream()
                                              .filter(Objects::nonNull)
                                              .map(restfulResolverProvider -> restfulResolverProvider.createRequestResolver(project))
                                              .filter(Objects::nonNull)
                                              .filter(o -> enabledWebFrameworks.contains(o.getFrameworkName()))
                                              .filter(RequestResolver::checkConfig)
                                              .sorted(Comparator.comparing(RequestResolver::getScanType))
                                              .collect(Collectors.toList());
    }

    public static List<RestItem> buildRequestItemList(@NotNull Project project) {
        boolean displayApiGroupUsingFileName = CommonSettingComponent.getInstance(project).getState()
                                                                     .isDisplayApiGroupUsingFileName();
        List<RequestResolver> requestResolvers = getEnabledRequestResolvers(project);
        return requestResolvers.stream()
                               .map(resolver -> resolver.findRestItemInProject(project))
                               .filter(Objects::nonNull)
                               .flatMap(Collection::stream)
                               .filter(Objects::nonNull)
                               .peek(item -> {
                                   if (displayApiGroupUsingFileName && item instanceof PsiRestItem) {
                                       PsiFile containingFile = ((PsiRestItem) item).getPsiElement().getContainingFile();
                                       if (containingFile != null) {
                                           item.setModuleName(FileUtils.removeFileSuffix(containingFile.getName()));
                                       }
                                   }
                               })
                               .filter(item -> item.getModuleName() != null && item.getUrl() != null)
                               .collect(Collectors.toList());
    }

    @NotNull
    public static String getCombinedPath(@NotNull String typePath, @NotNull String methodPath) {
        if (typePath.isEmpty()) {
            typePath = "/";
        } else if (!typePath.startsWith("/")) {
            typePath = "/".concat(typePath);
        }

        if (!methodPath.isEmpty()) {
            if (!methodPath.startsWith("/") && !typePath.endsWith("/")) {
                methodPath = "/".concat(methodPath);
            }
        }

        return (typePath + methodPath).replace("//", "/");
    }
}