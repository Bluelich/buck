/*
 * Copyright 2012-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.android;

import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.rules.AddToRuleKey;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildTargetSourcePath;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.SourcePathRuleFinder;
import com.facebook.buck.rules.args.Arg;
import com.facebook.buck.shell.Genrule;
import com.facebook.buck.step.ExecutionContext;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;

/**
 * A specialization of a genrule that specifically allows the modification of apks. This is useful
 * for processes that modify an APK, such as zipaligning it or signing it.
 *
 * <p>The generated APK will be at <code><em>rule_name</em>.apk</code>.
 *
 * <pre>
 * apk_genrule(
 *   name = 'fb4a_signed',
 *   apk = ':fbandroid_release'
 *   deps = [
 *     '//java/com/facebook/sign:fbsign_jar',
 *   ],
 *   cmd = '${//java/com/facebook/sign:fbsign_jar} --input $APK --output $OUT'
 * )
 * </pre>
 */
public class ApkGenrule extends Genrule implements HasInstallableApk {

  @AddToRuleKey private final BuildTargetSourcePath apk;
  private final HasInstallableApk hasInstallableApk;
  private final boolean isCacheable;

  ApkGenrule(
      BuildTarget buildTarget,
      ProjectFilesystem projectFilesystem,
      BuildRuleParams params,
      SourcePathRuleFinder ruleFinder,
      List<SourcePath> srcs,
      Optional<Arg> cmd,
      Optional<Arg> bash,
      Optional<Arg> cmdExe,
      Optional<String> type,
      SourcePath apk,
      boolean isCacheable) {
    super(
        buildTarget,
        projectFilesystem,
        params,
        srcs,
        cmd,
        bash,
        cmdExe,
        type,
        /* out */ buildTarget.getShortNameAndFlavorPostfix() + ".apk");

    Preconditions.checkState(apk instanceof BuildTargetSourcePath);
    this.apk = (BuildTargetSourcePath) apk;
    BuildRule rule = ruleFinder.getRule(this.apk);
    Preconditions.checkState(rule instanceof HasInstallableApk);
    this.hasInstallableApk = (HasInstallableApk) rule;
    this.isCacheable = isCacheable;
  }

  public HasInstallableApk getInstallableApk() {
    return hasInstallableApk;
  }

  @Override
  public ApkInfo getApkInfo() {
    return ApkInfo.builder()
        .from(hasInstallableApk.getApkInfo())
        .setApkPath(getSourcePathToOutput())
        .build();
  }

  @Override
  public boolean isCacheable() {
    return isCacheable;
  }

  @Override
  protected void addEnvironmentVariables(
      SourcePathResolver pathResolver,
      ExecutionContext context,
      ImmutableMap.Builder<String, String> environmentVariablesBuilder) {
    super.addEnvironmentVariables(pathResolver, context, environmentVariablesBuilder);
    // We have to use an absolute path, because genrules are run in a temp directory.
    String apkAbsolutePath =
        pathResolver.getAbsolutePath(hasInstallableApk.getApkInfo().getApkPath()).toString();
    environmentVariablesBuilder.put("APK", apkAbsolutePath);
  }
}
