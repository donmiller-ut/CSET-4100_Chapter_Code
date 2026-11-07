SET ROLE northstar_owner;

INSERT INTO northstar.category (category_id, name) VALUES
    (2, 'Hydration'),
    (3, 'Camp Kitchen');

INSERT INTO northstar.product (
    product_id, name, description, price, active, category_id
) VALUES
    (43, 'Insulated Bottle', 'One-liter stainless bottle', 24.95, true, 2),
    (44, 'Collapsible Cup', 'Compact silicone trail cup', 8.50, true, 3),
    (45, 'Camp Stove', 'Retired compact stove', 59.00, false, 3),
    (46, 'Water Filter', 'Backcountry microfilter', 34.75, true, 2);

SELECT setval('northstar.category_category_id_seq', 3, true);
SELECT setval('northstar.product_product_id_seq', 46, true);

RESET ROLE;
