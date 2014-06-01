/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.config;

import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.internal.gosu.util.RabinKarpHash;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.module.IModule;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractPlatformHelper extends BaseService implements IPlatformHelper {

  private static final String[] IGNORE_DIRECTORY_PATTERNS = new String[]{
      "multiapp/ExtendContactEntityWithArraysConfig",
      "multiapp/MultiAppContactAutosycTestConfig",
      "test-config",
      "pcf-config",
      "rule-config",
      "modifiers-test-config",
      "shared-configs",
      "shared-modules",
      "config/web/templates",
      "config/templates",
      "exception/test/config",
      "testConfigPluginCallbackHandler/config",
      "premium-report-test/config",
      "typeloader/test-files",
      "testConfigFinancialsEscalationTest/config",
      "testConfigReserveThresholdValidationDisabled/config",
      "testConfigSetApprovalToInactiveUser/config",
      "testConfigRecoveryEventFiredRuleLogging/config",
      "ClaimCreateRecoveryConfig/config",
      "bundle/validation/preupdate/config/rules",
      "testConfigI18NWorkflowLocalizationStepIDInJapanese/config",
      "testConfigI18NWorkflowLocalization/config",
      "testConfigI18NWorkflowLogLocalization/config",
      "testConfigI18NWorkflowNoLocalization/config",
      "testConfigI18NWorkflowLocalizationStepNameWithParentLocale/config",
      "testConfigI18NWorkflowSimpleActivityStepWithTimeoutBranch1/config",
      "testConfigFinancialsValidationTest/config",
      "testConfigRequiresApprovalForAllManualChecks/config",
      "testConfigCreateDocumentSynchronouslyRuleEnabled/config",
      "ClaimCreateCheckFinalConfig/config",
      "ClaimCreateCheckPartialConfig/config",
      "ExposureCreateRecoveryConfig/config",
      "async/async_config/config/rules",
      "config-messaging/config/rules",
      "parent/config/rules",
      "child/config/rules",
      "rule-with-exception-test/config",
      "config_qplexorstartorrestarttestplugin_message_rule_config/config",
      "plugin_message_rule2_config/config",
      "plugin_message_rule_ForAssignmentTest-config/config",
      "test_config_WFStepLocalizationDefaultLocale/config",
      "config_multisenderthreadswithexceptionwhensend/config",
      "rule_messaging-config/config",
      "rule_message_plugins-config/config",
      "messageitemruleconfig/config",
      "rules/ruleshadow/config",
      "populatemessagedescriptioninruleconfig/config",
      "WorkflowVersionNumber_rule_workflow_Config/config",
      "config-contact-messaging/config",
      "config_multisenderthreads/config",
      "/impl/testQueryColumnsTest/config/"
  };

  private static RabinKarpHash HASH = new RabinKarpHash(IGNORE_DIRECTORY_PATTERNS);

  private Set<String> EXTENSIONS = new HashSet<String>(Arrays.asList(new String[]{
      "pcf", "eti", "eix", "etx", "tti", "ttx", "tix", "gr", "grs"
  }));

  public boolean isConfigFile(IFile file) {
    final String extension = file.getExtension();
    if (extension != null) {
      return EXTENSIONS.contains(extension);
    }
    return false;
  }

  public boolean isPathIgnored(String relativePath) {
    if (relativePath == null) {
      return true;
    }

    final IFile file = CommonServices.getFileSystem().getIFile(new File(relativePath));
    if (file != null && file.exists() && isConfigFile(file)) {
      final IModule module = TypeSystem.getExecutionEnvironment().getModule(file);
      if (module != null) {
        for (IDirectory dir : module.getSourcePath()) {
          if ("config".equals(dir.getName()) && file.isDescendantOf(dir)) {
            return false;
          }
        }
      }
      //System.out.println("Ignoring: " + relativePath);
      return true;
    }

//    for (String pattern : IGNORE_DIRECTORY_PATTERNS) {
//      if (relativePath.contains(pattern)) {
//        return true;
//      }
//    }
    return HASH.matches(relativePath);
  }

  @Override
  public File getIndexFile(String id) {
    throw new RuntimeException("Not supported");
  }

  @Override
  public String getIDEACachesDir() {
    throw new RuntimeException("Not supported");
  }

  @Override
  public File getIDEACachesDirFile() {
    throw new RuntimeException("Not supported");
  }

  @Override
  public File getIDEACorruptionMarkerFile() {
    throw new RuntimeException("Not supported");
  }
}
