SET ROLE northstar_owner;
-- V4 inherited default write privileges from the original bootstrap.
-- This milestone reads Customer profiles but has no Customer write use case.
REVOKE INSERT, UPDATE, DELETE ON TABLE northstar.customer FROM northstar_app;
RESET ROLE;
