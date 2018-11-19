INSERT INTO "db.dbmodel::Sample"("ID", "Explanation", "IsDeleted", "KeyFigure", "Note","AssignedParent_ID")  VALUES(1000,'explanation',0,'Standardhauptgruppe','note',null);
INSERT INTO "db.dbmodel::Sample"("ID", "Explanation", "IsDeleted", "KeyFigure", "Note","AssignedParent_ID")  VALUES(1001,'explanation',0,'Standardobergruppe','note',1000);
INSERT INTO "db.dbmodel::Sample"("ID", "Explanation", "IsDeleted", "KeyFigure", "Note","AssignedParent_ID")  VALUES(1002,'explanation',0,'Standardgruppe','note',1001);
