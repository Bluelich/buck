/*
 * Copyright 2013-present Facebook, Inc.
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

package com.facebook.buck.rules;

import com.facebook.buck.graph.DefaultImmutableDirectedAcyclicGraph;
import com.facebook.buck.graph.ImmutableDirectedAcyclicGraph;
import com.facebook.buck.graph.MutableDirectedGraph;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

public class AbstractDependencyVisitors {
  private AbstractDependencyVisitors() {
  }

  /**
   * Given dependencies in inputs builds graph of transitive dependencies filtering them by
   * instanceOf T.
   *
   * @param inputs      initial dependencies from which to build transitive closure
   * @param filter      predicate to determine whether a node should be included
   * @param traverse    predicate to determine whether this node should be traversed
   * @param <T>         class to fitler on
   * @return            filtered BuildRule DAG of transitive dependencies
   *
   * @see com.facebook.buck.rules.BuildRule
   */
  public static <T> ImmutableDirectedAcyclicGraph<BuildRule> getBuildRuleDirectedGraphFilteredBy(
      final Iterable<? extends BuildRule> inputs,
      final Predicate<Object> filter,
      final Predicate<Object> traverse) {

    // Build up a graph of the inputs and their transitive dependencies, we'll use the graph
    // to topologically sort the dependencies.
    final MutableDirectedGraph<BuildRule> graph = new MutableDirectedGraph<>();
    AbstractDependencyVisitor visitor = new AbstractDependencyVisitor(inputs) {
      @Override
      public ImmutableSet<BuildRule> visit(BuildRule rule) {
        if (filter.apply(rule)) {
          graph.addNode(rule);
          for (BuildRule dep : rule.getDeps()) {
            if (traverse.apply(dep) && filter.apply(dep)) {
              graph.addEdge(rule, dep);
            }
          }
        }
        return traverse.apply(rule) ? rule.getDeps() : ImmutableSet.<BuildRule>of();
      }
    };
    visitor.start();
    return new DefaultImmutableDirectedAcyclicGraph<>(graph);
  }
}
