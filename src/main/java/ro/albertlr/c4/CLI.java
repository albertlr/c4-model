/*-
 * #%L
 * jira-cli
 *  
 * Copyright (C) 2019 László-Róbert, Albert (robert@albertlr.ro)
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ro.albertlr.c4;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import ro.albertlr.c4.processor.Processor;

import java.util.Arrays;

@Slf4j
public class CLI {

    public static final String ISSUE_TYPE_CUSTOMER_DEFECT = "Customer Defect";
    public static final String ISSUE_TYPE_DEFECT = "Defect";
    public static final String ISSUE_TYPE_FEATURE_STORY = "Feature Story";

    public static final String ISSUE_E2E = "End-to-end Test";
    public static final String DEPENDS_ON_LINK = "Depends On";
    public static final String TESTED_BY_LINK = "Tests Writing";

    public static void execute(String... args) {
        try {
            main(args);
        } catch (Exception e) {
            log.error("Could not execute main({})", Arrays.toString(args));
        }
    }

    public static void main(String[] args) throws Exception {
        CommandLine cli = Params.cli(args);

        String jiraSourceKey = Params.getParameter(cli, Params.SOURCE_ARG);
        final Processor.Name action = Processor.Name.from(Params.getParameter(cli, Params.ACTION_ARG));

        try (final Context context = new Context();) {
            switch (action) {
//                case GET: {
//                    Issue issue = action.execute(jira, jiraSourceKey);
//
//                    IssueLogger.fullLog(log, issue);
//                }
//                break;
                default:
                    Params.printUsage();
                    break;
            }
        }
    }



}
