--liquibase formatted sql
--changeset cosmic:default-storage-slots-eight
UPDATE setting_catalog
SET default_value = '8',
    compatibility_note = 'Existing account storage rows keep their current slot count. New storage rows default to 8 slots unless YAML or Server CMS overrides specify another value. Current storage cap is 48.'
WHERE setting_key = 'server.world.defaultStorageSlots';
