-- This script should be run on a freshly created database to add in configuration data for a clean harvester install
-- If you intend to add new steps, you WILL need to change this file. The only thing you need to keep in mind is to make sure the 
-- new rows have unique ids, and make sure that you keep this file in sync with your database.


--This bit provides the link between the client and the OAI harvesting code on the processor. Extremely important.
INSERT INTO step VALUES(4, 'harvester.processor.steps.HarvesterThroughWS', 'OAI', '', 'xml', 0, 'Harvests OAI');
INSERT INTO parameterinformation VALUES( 6, 4, 'Base URL', '', 'String', 1, 'The Base URL of the OAI data source', null);
INSERT INTO parameterinformation VALUES( 7, 4, 'Set', '', 'String', 0, 'Harvest only a subset of data in the repository', null);
INSERT INTO parameterinformation VALUES( 8, 4, 'Metadata Prefix', 'oai_dc', 'String', 1, 'The data format to harvest', null);
INSERT INTO profilestep VALUES( 4, null,4, 0, 1, 'Harvest through OAI', 0, 1);
INSERT INTO profilestepparameter VALUES (6, 4, 6, 1, 'http://export.arxiv.org/oai2');
INSERT INTO profilestepparameter VALUES (7, 4, 7, 1, 'cs');
INSERT INTO profilestepparameter VALUES (8, 4, 8, 1, 'oai_dc');

-- encoding stuff, used as one of the inputs to the OAI step
INSERT INTO parameterinformation VALUES (12, 4, 'Encoding', 'autodetect', 'String', 0, 'An encoding to use', null);
INSERT INTO parameteroption VALUES (9, 12, 'autodetect', '--autodetect--');
INSERT INTO parameteroption VALUES (10, 12, 'ISO-8859-1', ' Latin-1/ISO-8859-1 - common 8bit European/English');
INSERT INTO parameteroption VALUES (11, 12, 'UTF-8', 'UTF-8 - Standard Unicode, expected OAI encoding');
INSERT INTO parameteroption VALUES (12, 12, 'GB18030', 'GB18030 - chinese unicode equivalent');
INSERT INTO parameteroption VALUES (13, 12, 'GBK', 'GBK - chinese standard extended');
INSERT INTO parameteroption VALUES (14, 12, 'GB2312', 'GB2312 - chinese standard');
INSERT INTO parameteroption VALUES (15, 12, 'KOI8-R', 'KOI8-R - Russian');
INSERT INTO parameteroption VALUES (16, 12, 'windows-1250', 'windows-1250 - Windows Eastern European');
INSERT INTO parameteroption VALUES (17, 12, 'windows-1251', 'windows-1251 - Windows Cyrillic');
INSERT INTO parameteroption VALUES (18, 12, 'windows-1252', 'windows-1252 - Windows Latin-1');
INSERT INTO parameteroption VALUES (19, 12, 'windows-1253', 'windows-1253 - Windows Greek');
INSERT INTO parameteroption VALUES (20, 12, 'windows-1254', 'windows-1254 - Windows Turkish');
INSERT INTO parameteroption VALUES (21, 12, 'windows-1257', 'windows-1257 - Windows Baltic');
INSERT INTO parameteroption VALUES (22, 12, 'UTF-16', 'UTF-16 - Unicode autodetect endian');
INSERT INTO parameteroption VALUES (23, 12, 'UTF-16BE', 'UTF-16BE - Unicode big endian');
INSERT INTO parameteroption VALUES (24, 12, 'UTF-16LE', 'UTF-16LE - Unicode little endian');


INSERT INTO step VALUES(7, 'harvester.processor.steps.Validator', 'Validator', 'xml', 'xml', 2, 'Performs a validation');
INSERT INTO parameterinformation VALUES(11, 7, 'schema' , '', 'String', 1, 'The schema document to validate against', null);

INSERT INTO step VALUES(50, 'harvester.processor.steps.CheckRequired', 'Check for Required Fields', 'xml', 'xml', 2, 'Fails any records that do not have the required fields');
INSERT INTO parameterinformation VALUES(20, 50, 'Check Fields', '', 'nested', 0, 'fields to check', null);
INSERT INTO parameterinformation VALUES(21, null, 'Field Name', '', 'xpath', 1, 'xpath Expression matching one or more fields', 20);
INSERT INTO parameterinformation VALUES(22, null, 'Required Value', '', 'regex', 0, 'regular expression that the value of the field must conform to', 20);
INSERT INTO parameterinformation VALUES(49, null, 'Match all occurrences', '', 'Boolean', 0, 'Match All occurrences', 20);

INSERT INTO step VALUES(51, 'harvester.processor.steps.CheckRepeated', 'Check Fields are Not Repeated', 'xml', 'xml', 2, 'Fails records that contain the specified repeated fields');
INSERT INTO parameterinformation VALUES(23, 51, 'Check Fields', '', 'nested', 0, 'fields to check', null);
INSERT INTO parameterinformation VALUES(24, null, 'Field Name', '', 'xpath', 1, 'xpath Expression matching one or more fields', 23);
INSERT INTO parameterinformation VALUES(25, null, 'Match Value', 'http*', 'regex', 0, 'regular expression that only one of the xpath matches may match', 23);

INSERT INTO step VALUES(58, 'harvester.processor.steps.AddField2', 'Add Field', 'xml', 'xml', 3, 'Adds a field to the xml record');
INSERT INTO parameterinformation VALUES(44, 58, 'Add', '', 'String', 0, 'Add options', null);
INSERT INTO parameterinformation VALUES(45, 58, 'New Field Name', '', 'String', 1, 'Field name of new field', null);
INSERT INTO parameterinformation VALUES(46, 58, 'New Field Value', '', 'String', 0, 'value of new field', null);
INSERT INTO parameterinformation VALUES(47, 58, 'Match Field Name', '', 'xpath', 0, 'xpath that matches a field already in the tree', null);
INSERT INTO parameterinformation VALUES(48, 58, 'Value Matches', '', 'regex', 0, 'if matched field matches this, then add the new field', null);

INSERT INTO step VALUES(53, 'harvester.processor.steps.DeleteField', 'Delete Field', 'xml', 'xml', 3, 'Deletes a field from a xml record');
INSERT INTO parameterinformation VALUES(29, 53, 'Fields', '', 'nested', 0, 'fields to remove', null);
INSERT INTO parameterinformation VALUES(30, null, 'Field Name', '', 'xpath', 1, 'xpath Expression matching one or more fields', 29);
INSERT INTO parameterinformation VALUES(31, null, 'Field Value', '', 'regex', 0, 'regular expression that the values of the matched fields should match', 29);

INSERT INTO step VALUES(54, 'harvester.processor.steps.ConvertValue', 'Convert Value', 'xml', 'xml', 3, 'Converts values in an xml record');
INSERT INTO parameterinformation VALUES(32, 54, 'Field Name', '', 'xpath', 1, 'xpath Expression matching one or more fields', null);
--NOTE: changed from 33 to 36 to make migration easier
INSERT INTO parameterinformation VALUES(36, 54, 'Rules', '', 'String', 0, 'Rule data', null);
INSERT INTO parameterinformation VALUES(50, 54, 'mappingfile', '', 'String', 0, 'mappingfile', null);

INSERT INTO step VALUES(56, 'harvester.processor.steps.SplitField', 'Split Field', 'xml', 'xml', 3, 'Splits a field in a xml record');
INSERT INTO parameterinformation VALUES(41, 56, 'Fields', '', 'nested', 0, 'fields to remove', null);
INSERT INTO parameterinformation VALUES(42, null, 'Field Name', '', 'xpath', 1, 'xpath Expression matching one or more fields', 41);
INSERT INTO parameterinformation VALUES(43, null, 'Delimiter', '', 'regex', 1, 'the delimiter string', 41);

--This is the link between the client and the processor for the ArrowLoader step. Extremely Important.
--INSERT INTO step VALUES(57, 'harvester.processor.steps.ArrowLoader', 'Arrow', 'xml', null, 1, 'Loads into arrow');
--INSERT INTO profilestep VALUES (12, null, 57, null, 1, 'Arrow', 0, 1);

-- People loader
--INSERT INTO step VALUES(61, 'harvester.processor.steps.PeopleLoader', 'People', 'xml', null, 1, 'Loads into People');
--INSERT INTO profilestep VALUES (15, null, 61, null, 1, 'People', 0, 1);

--Example loader
INSERT INTO step VALUES(63, 'harvester.processor.steps.ExampleLoader', 'Example', 'xml', null, 1, 'A template for load steps');
INSERT INTO profilestep VALUES (16, null, 63, null, 1, 'Example', 0, 1);

INSERT INTO step VALUES(59, 'harvester.processor.steps.Clusterer', 'Generate Clusters', 'xml', 'xml', 3, 'Generates a cluster view of the data');
INSERT INTO parameterinformation VALUES(51, 59, 'Fields', '', 'nested', 0, 'fields to use', null);
INSERT INTO parameterinformation VALUES(52, null, 'Field Name', '', 'xpath', 1, 'xpath Expression matching one or more fields', 51);
INSERT INTO parameterinformation VALUES(53, null, 'Split on Spaces', '', 'Boolean', 0, 'should the text in the field be split into words for clustering?', 51);

INSERT INTO step VALUES(60, 'harvester.processor.steps.ClassStep', 'Java Transformation', 'xml', 'xml', 3, 'Apply some code to the record');
INSERT INTO parameterinformation VALUES(54, 60, 'converter', '', 'String', 2, 'The java class containing the transformation code', null);
INSERT INTO parameteroption VALUES (25, 54, 'harvester.processor.steps.MarcConverter', 'MarcXML to EAC Transformer');
INSERT INTO parameteroption VALUES (26, 54, 'harvester.processor.steps.MapsSplit', 'Splits Maps Records');
INSERT INTO parameteroption VALUES (27, 54, 'harvester.processor.steps.DCToEAC', 'Converts DC to EAC');

INSERT INTO step VALUES(62, 'harvester.processor.steps.XSLTTranslator', 'XSLT Translator', 'xml', 'xml', 3, 'Performs an xml transformation using a XSLT stylesheet');
INSERT INTO parameterinformation VALUES(55, 62, 'fileid', '42', 'Integer', 2, 'The XSLT document describing the transformation', null);
