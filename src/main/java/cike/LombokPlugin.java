package cike;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.JavaFormatter;
import org.mybatis.generator.api.PluginAdapter;

import java.util.*;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;


public class LombokPlugin extends PluginAdapter {


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
  public boolean modelBaseRecordClassGenerated(
      TopLevelClass topLevelClass,
      IntrospectedTable introspectedTable
  ) {
    topLevelClass.getAnnotations().removeIf(it -> it.contains("@Generated"));
    topLevelClass.getImportedTypes().removeIf( it -> it.getFullyQualifiedName().contains("Generated"));
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
    String targetPackage = introspectedTable.getContext().getJavaClientGeneratorConfiguration().getTargetPackage();

    String myBatis3JavaMapperType = introspectedTable.getMyBatis3JavaMapperType();

    FullyQualifiedJavaType fullyQualifiedJavaType = new FullyQualifiedJavaType(myBatis3JavaMapperType);
    Interface topLevelClass = new Interface(fullyQualifiedJavaType.getPackageName() + "." + fullyQualifiedJavaType.getShortName() + "Self");
    topLevelClass.setVisibility(JavaVisibility.PUBLIC);

    topLevelClass.getImportedTypes().add(fullyQualifiedJavaType);
    topLevelClass.getSuperInterfaceTypes().add(fullyQualifiedJavaType);

    GeneratedJavaFile generatedJavaFile = new GeneratedJavaFile(topLevelClass, targetProject,
        "utf-8", javaFormatter);
    return List.of(generatedJavaFile);
  }
}
