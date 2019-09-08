DELETE FROM PUBLIC.TRANSACTION;
DELETE FROM PUBLIC.BLOCK;
delete from version;

INSERT INTO PUBLIC.BLOCK
(DB_ID,         ID,                HEIGHT,      VERSION,   TIMESTAMP,  PREVIOUS_BLOCK_ID,  TOTAL_AMOUNT,        TOTAL_FEE,   PAYLOAD_LENGTH,   PREVIOUS_BLOCK_HASH,                                                   CUMULATIVE_DIFFICULTY,  BASE_TARGET,    NEXT_BLOCK_ID,               GENERATION_SIGNATURE,                                                   BLOCK_SIGNATURE,                                                                                                                        PAYLOAD_HASH,                                                           GENERATOR_ID,       TIMEOUT) VALUES
(1	        ,-107868771406622438   ,0	        ,-1         ,0	        , null                  ,0	            ,0	                ,0          ,X'0000000000000000000000000000000000000000000000000000000000000000'	,X'00'	                ,5124095	    , 8235640967557025109		,X'bc26bb638c9991f88fa52365591e00e22d3e9f9ad721ca4fe1683c8795a037e5'	,X'00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000'	,X'0000000000000000000000000000000000000000000000000000000000000000'	, 1739068987193023818	,0   ),
(104830	    ,-468651855371775066   ,2994	 	,3	        ,9200       , 9108206803338182346   ,0	            ,100000000	        ,1255       ,X'cabec48dd4d9667e562234245d06098f3f51f8dc9881d1959496fd73d7266282'    ,X'026543d9a8161629'    ,9331842	    ,-1868632362992335764	    ,X'002bc5d6612e35e00e0a8141382eab45c20243d9dad4823348bfe85147b95acf'	,X'e920b526c9200ae5e9757049b3b16fcb050b416587b167cb9d5ca0dc71ec970df48c37ce310b6d20b9972951e9844fa817f0ff14399d9e0f82fde807d0957c31'    ,X'37f76b234414e64d33b71db739bd05d2cf3a1f7b344a88009b21c89143a00cd0'	, 9211698109297098287   ,0   ),
(105015	    ,-7242168411665692630  ,2995	 	,3	        ,13800      ,-3475222224033883190   ,0	            ,100000000	        ,1257       ,X'cadbeabccc87c5cf1cf7d2cf7782eb34a58fb2811c79e1d0a3cc60099557f4e0'    ,X'026601a7a1c313ca'    ,7069966	    , 5841487969085496907		,X'fbf795ff1d4138f11ea3d38842aa319f8a21589eb46ea8cfc71850f8b55508ef'	,X'978b50eb629296b450f5298b61601685cbe965d4995b03707332fdc335a0e708a453bd7969bd9d336fbafcacd89073bf55c3b3395acf6dd0f3204c2a5d4b402e'    ,X'2cba9a6884de01ff23723887e565cbde21a3f5a0a70e276f3633645a97ed14c6'	, 9211698109297098287   ,0   ),
(1641706	,-6746699668324916965  ,2996	 	,5	        ,18400      , 2069655134915376442	,0	            ,200000000	        ,207	    ,X'3ab5313461e4b81c8b7af02d73861235a4e10a91a400b05ca01a3c1fdd83ca7e'	,X'02dfb5187e88edab'	,23058430050	,-3540343645446911906		,X'dd7899249f0adf0d7d6f05055f7c6396a4a8a9bd1d189bd5e2eed647f8dfcc0b'	,X'4b415617a8d85f7fcac17d2e9a1628ebabf336285acdfcb8a4c4a7e2ba34fc0f0e54cd88d66aaa5f926bc02b49bc42b5ae52870ba4ac802b8276d1c264bec3f4'	,X'18fa6d968fcc1c7f8e173be45492da816d7251a8401354d25c4f75f27c50ae99'	, 5564664969772495473	,1   ),
(1641707	,-3540343645446911906  ,2997	 	,6	        ,22998      ,-6746699668324916965	,0	            ,0	                ,0	        ,X'1b613faf65e85ea257289156c62ec7d45684759ebceca59e46f8c94961b7a09e'	,X'02dfb518ae37f5ac'	,23058430050	, 2729391131122928659		,X'facad4c1e0a7d407e0665393253eaf8e9f1e1e7b26e035687939897eaec9efe3'	,X'f35393c0ff9721c84123075988a278cfdc596e2686772c4e6bd82751ecf06902a942f214c5afb56ea311a8d48dcdd2a44258ee03764c3e25ad1796f7d646185e'	,X'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'	,-902424482979450876	,4   ),
  ;
--last block from testnet1
INSERT INTO PUBLIC.TRANSACTION
(DB_ID,     ID,                      HEIGHT,      BLOCK_ID,            BLOCK_TIMESTAMP,    DEADLINE, RECIPIENT_ID,     TRANSACTION_INDEX, AMOUNT,             FEE,            FULL_HASH,                                                                     SIGNATURE,                                                                                                                                  TIMESTAMP, TYPE, SUBTYPE, SENDER_ID,                SENDER_PUBLIC_KEY,                                                   REFERENCED_TRANSACTION_FULL_HASH,                                                     PHASED, VERSION, HAS_MESSAGE, HAS_ENCRYPTED_MESSAGE, HAS_PUBLIC_KEY_ANNOUNCEMENT, EC_BLOCK_HEIGHT,   EC_BLOCK_ID,            HAS_ENCRYPTTOSELF_MESSAGE, HAS_PRUNABLE_MESSAGE, HAS_PRUNABLE_ENCRYPTED_MESSAGE, HAS_PRUNABLE_ATTACHMENT, ATTACHMENT_BYTES) VALUES
  (150      ,3444674909301056677	  ,2994	     ,-468651855371775066       ,9200	       ,1440,	null	                ,0	        ,0	                ,2500000000000	,X'a524974f94f1cd2fcc6f17193477209ca5821d37d391e70ae668dd1c11dd798e'	      ,X'375ef1c05ae59a27ef26336a59afe69014c68b9bf4364d5b1b2fa4ebe302020a868ad365f35f0ca8d3ebaddc469ecd3a7c49dec5e4d2fad41f6728977b7333cc'	    ,35073712	    ,0	   ,1	    ,-8315839810807014152	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'6400000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e'	    ,FALSE	    ,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14399	        ,-5416619518547901377	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (175      ,2402544248051582903	  ,2994	     ,-468651855371775066       ,9200	       ,1440,	null	                ,1	        ,0	                ,1000000000000	,X'b7c745ae438d57212270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d'	      ,X'fc6f11f396aa20717c9191a1fb25fab0681512cc976c935db1563898aabad90ffc6ced28e1b8b3383d5abb55928bbb122a674dc066ab8b0cc585b9b4cdbd8fac'	    ,35075179	    ,0	   ,1	    ,-8315839810807014152	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'6500000000000000cc6f17193477209ca5821d37d391e70ae668dd1c11dd798e'	    ,FALSE	    ,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14405	        ,-2297016555338476945	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (200      ,5373370077664349170	  ,2995	     ,-7242168411665692630      ,13800	       ,1440,	457571885748888948	    ,0	        ,100000000000000000	,100000000	    ,X'f28be5c59d0b924ab96d5e9f64e51c597513717691eeeeaf18a26a864034f62c'	      ,X'8afd3a91d0e3011e505e0353b1f7089c0d401672f8ed5d0ddc2107e0b130aa0bdd17f03b2d75eed8fcc645cda88b5c82ac1b621c142abad9b1bb95df517aa70c'	    ,35078473	    ,0	   ,1	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'b7c745ae438d57212270a2b00e3f70fb5d5d8e0da3c7919edd4d3368176e6f2d'                  ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
  (500      ,-780794814210884355	  ,2996	     ,-6746699668324916965      ,18400	       ,1440,	6110033502865709882	    ,1	        ,100000000000000000	,100000000	    ,X'fd3c7ed8400f2af5cca5a1f825f9b918be00f35406f70b108b6656b299755558'	      ,X'240b0a1ee9f63f5c3cb914b42584da1388b9d048a981f1651ac85dd12f12660c29782100c03cbe8491bdc831aa27f6fd3a546345b3da7860c56e6ba431550517'	    ,35078473	    ,0	   ,1	    ,9211698109297098287	 ,X'bf0ced0472d8ba3df9e21808e98e61b34404aad737e2bae1778cebc698b40f37' ,X'f28be5c59d0b924ab96d5e9f64e51c597513717691eeeeaf18a26a864034f62c'	                ,FALSE		,1	,FALSE	        ,FALSE	            ,FALSE	                        ,14734	        ,2621055931824266697	,FALSE	                    ,FALSE	                ,FALSE	                        ,FALSE,                 null),
;
INSERT into version values (24);