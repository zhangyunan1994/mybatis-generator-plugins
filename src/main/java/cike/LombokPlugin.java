package cike;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.JavaFormatter;
import org.mybatis.generator.api.PluginAdapter;

import java.util.*;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.java.render.RenderingUtilities;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.mybatis.generator.logging.Log;
import org.mybatis.generator.logging.LogFactory;


public class LombokPlugin extends PluginAdapter {

  static Log log = LogFactory.getLog(LombokPlugin.class);

  @Override
  public boolean validate(List<String> warnings) {
    return true;
  }

  @Override
  public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass,
    IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
    return false;
  }

  @Override
  public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass,
    IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
    return false;
  }

  @Override
  public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
    topLevelClass.getAnnotations().removeIf(it -> it.contains("@Generated"));
    topLevelClass.getImportedTypes().removeIf(it -> it.getFullyQualifiedName().contains("Generated"));
    topLevelClass.getAnnotations().add("@Data");
    topLevelClass.getAnnotations().add("@Accessors(chain = true)");
    topLevelClass.getImportedTypes().add(new FullyQualifiedJavaType("lombok.Data"));
    topLevelClass.getImportedTypes().add(new FullyQualifiedJavaType("lombok.experimental.Accessors"));
    return true;
  }

  @Override
  public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn,
    IntrospectedTable introspectedTable, ModelClassType modelClassType) {
    field.getAnnotations().removeIf(it -> it.contains("@Generated"));
    return true;
  }

  @Override
  public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
    JavaFormatter javaFormatter = introspectedTable.getContext().getJavaFormatter();
    String targetProject = introspectedTable.getContext().getJavaClientGeneratorConfiguration().getTargetProject();

    List<GeneratedJavaFile> generatedJavaFiles = new ArrayList<>();

    DefaultShellCallback defaultShellCallback = new DefaultShellCallback(false);

    // 生成自定义 Mapper
    CompilationUnit selfMapper = null;
    try {
      selfMapper = generateSelfMapper(introspectedTable);
      GeneratedJavaFile selfMapperJavaFile = new GeneratedJavaFile(selfMapper, targetProject, "utf-8", javaFormatter);
      File directory = defaultShellCallback.getDirectory(targetProject, selfMapper.getType().getPackageName());
      Path targetFile = directory.toPath().resolve(selfMapperJavaFile.getFileName());
      if (Files.exists(targetFile)) {
        log.info("自定义 Mapper 文件已存在: " + selfMapperJavaFile.getFileName());
      }
      else {
        generatedJavaFiles.add(selfMapperJavaFile);
      }
    }
    catch (ShellException e) {
      log.error("自定义 Mapper 异常: ", e);
    }

    // 生成自定义 Repository
    try {
      CompilationUnit selfRepository = generateSelfRepository(introspectedTable, selfMapper);
      if (selfRepository != null) {
        GeneratedJavaFile selfRepositoryJavaFile = new GeneratedJavaFile(selfRepository, targetProject, "utf-8", javaFormatter);
        File directory = defaultShellCallback.getDirectory(targetProject, selfRepository.getType().getPackageName());
        Path targetFile = directory.toPath().resolve(selfRepositoryJavaFile.getFileName());
        if (Files.exists(targetFile)) {
          log.info("自定义 Repository 文件已存在: " + selfRepositoryJavaFile.getFileName());
        }
        else {
          generatedJavaFiles.add(selfRepositoryJavaFile);
        }
      }
    }
    catch (ShellException e) {
      log.error("自定义 Repository 异常: ", e);
    }

    return generatedJavaFiles;
  }

  private CompilationUnit generateSelfMapper(IntrospectedTable introspectedTable) {
    String myBatis3JavaMapperType = introspectedTable.getMyBatis3JavaMapperType();
    FullyQualifiedJavaType fullyQualifiedJavaType = new FullyQualifiedJavaType(myBatis3JavaMapperType);
    String shortName = fullyQualifiedJavaType.getShortName();
    shortName = shortName.substring(0, shortName.length() - 6);

    String packageName = fullyQualifiedJavaType.getPackageName().substring(0, fullyQualifiedJavaType.getPackageName().lastIndexOf("."));

    Interface topLevelClass = new Interface(packageName + ".selfmapper." + shortName + "SelfMapper");
    topLevelClass.setVisibility(JavaVisibility.PUBLIC);
    topLevelClass.addAnnotation("@Mapper");

    topLevelClass.getImportedTypes().add(fullyQualifiedJavaType);
    topLevelClass.getImportedTypes().add(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Mapper"));
    topLevelClass.getSuperInterfaceTypes().add(fullyQualifiedJavaType);
    return topLevelClass;
  }

  private CompilationUnit generateSelfRepository(IntrospectedTable introspectedTable, CompilationUnit selfMapper) {
    String baseRecordType = introspectedTable.getBaseRecordType();
    FullyQualifiedJavaType baseRecordTypeJavaType = new FullyQualifiedJavaType(baseRecordType);

    String packageName = baseRecordTypeJavaType.getPackageName().substring(0, baseRecordTypeJavaType.getPackageName().lastIndexOf("."));

    TopLevelClass topLevelClass = new TopLevelClass(packageName + "." + baseRecordTypeJavaType.getShortName() + "Repository");
    topLevelClass.setVisibility(JavaVisibility.PUBLIC);
    topLevelClass.addAnnotation("@Repository");
    topLevelClass.addAnnotation("@RequiredArgsConstructor");

    FullyQualifiedJavaType dynamicSqlSupportType = new FullyQualifiedJavaType(introspectedTable.getMyBatisDynamicSqlSupportType());

    topLevelClass.getImportedTypes().add(selfMapper.getType());
    topLevelClass.getImportedTypes().add(new FullyQualifiedJavaType("org.springframework.stereotype.Repository"));
    topLevelClass.getImportedTypes().add(new FullyQualifiedJavaType("lombok.RequiredArgsConstructor"));
    topLevelClass.getImportedTypes().add(new FullyQualifiedJavaType("java.util.List"));
    topLevelClass.getImportedTypes().add(dynamicSqlSupportType);
    topLevelClass.getImportedTypes().add(baseRecordTypeJavaType);
    topLevelClass.getStaticImports().add("org.mybatis.dynamic.sql.SqlBuilder.isEqualTo");

    Field selfMapperField = new Field("selfMapper", selfMapper.getType());
    selfMapperField.setVisibility(JavaVisibility.PRIVATE);
    selfMapperField.setFinal(true);

    topLevelClass.addField(selfMapperField);

    Method saveMethod = new Method("save");
    saveMethod.setVisibility(JavaVisibility.PUBLIC);
    saveMethod.addParameter(new Parameter(baseRecordTypeJavaType, "param"));
    saveMethod.addBodyLine("return selfMapper.insertSelective(param);");
    saveMethod.setReturnType(FullyQualifiedJavaType.getIntInstance());
    topLevelClass.addMethod(saveMethod);

    Method allMethod = new Method("all");
    allMethod.setVisibility(JavaVisibility.PUBLIC);
    allMethod.addBodyLine("return selfMapper.select(s -> s);");
    allMethod.setReturnType(new FullyQualifiedJavaType("java.util.List<" + baseRecordTypeJavaType.getShortName() + ">"));
    topLevelClass.addMethod(allMethod);

    List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
    if (primaryKeyColumns.size() == 1) {
      Method updateByIdMethod = new Method("updateById");
      updateByIdMethod.setVisibility(JavaVisibility.PUBLIC);
      updateByIdMethod.addParameter(new Parameter(baseRecordTypeJavaType, "param"));
      updateByIdMethod.addBodyLine("return selfMapper.updateByPrimaryKeySelective(param);");
      updateByIdMethod.setReturnType(FullyQualifiedJavaType.getIntInstance());
      topLevelClass.addMethod(updateByIdMethod);
    }

    if (primaryKeyColumns.size() == 1) {
      IntrospectedColumn primaryKeyColumn = primaryKeyColumns.getFirst();

      String actualColumnName = primaryKeyColumn.getActualColumnName();
      String capitalizedColumnName = Character.toUpperCase(actualColumnName.charAt(0)) + actualColumnName.substring(1);

      Method deleteByPrimaryKeyMethod = new Method("deleteBy" + capitalizedColumnName);
      deleteByPrimaryKeyMethod.setVisibility(JavaVisibility.PUBLIC);
      deleteByPrimaryKeyMethod.setReturnType(FullyQualifiedJavaType.getIntInstance());
      deleteByPrimaryKeyMethod.addParameter(new Parameter(primaryKeyColumns.getFirst().getFullyQualifiedJavaType(), actualColumnName));
      deleteByPrimaryKeyMethod.addBodyLine("return selfMapper.deleteByPrimaryKey(" + actualColumnName + ");");
      topLevelClass.addMethod(deleteByPrimaryKeyMethod);

      Method selectByPrimaryKeyMethod = new Method("selectBy" + capitalizedColumnName);
      selectByPrimaryKeyMethod.setVisibility(JavaVisibility.PUBLIC);
      selectByPrimaryKeyMethod.setReturnType(baseRecordTypeJavaType);
      selectByPrimaryKeyMethod.addParameter(new Parameter(primaryKeyColumns.getFirst().getFullyQualifiedJavaType(), actualColumnName));
      selectByPrimaryKeyMethod.addBodyLine("return selfMapper.selectByPrimaryKey(" + actualColumnName + ").orElse(null);");
      topLevelClass.addMethod(selectByPrimaryKeyMethod);
    }

    if (primaryKeyColumns.size() > 1) {
      for (IntrospectedColumn primaryKeyColumn : primaryKeyColumns) {
        String actualColumnName = primaryKeyColumn.getActualColumnName();
        String capitalizedColumnName = Character.toUpperCase(actualColumnName.charAt(0)) + actualColumnName.substring(1);

        Method deleteByMethod = new Method("deleteBy" + capitalizedColumnName);
        deleteByMethod.setVisibility(JavaVisibility.PUBLIC);
        deleteByMethod.setReturnType(FullyQualifiedJavaType.getIntInstance());
        deleteByMethod.addParameter(new Parameter(primaryKeyColumn.getFullyQualifiedJavaType(), actualColumnName));
        deleteByMethod.addBodyLine(
          "return selfMapper.delete(c -> c.where(" + dynamicSqlSupportType.getShortName() + "." + actualColumnName.toLowerCase()
            + ", isEqualTo(" + actualColumnName + ")));");
        topLevelClass.addMethod(deleteByMethod);
      }
    }

    return topLevelClass;
  }
}
