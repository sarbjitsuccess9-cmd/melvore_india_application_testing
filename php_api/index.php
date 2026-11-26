<?php
declare(strict_types=1);

require_once __DIR__ . '/includes/response.php';

MelvoreResponse::requireMethod('GET');
MelvoreResponse::ok([
    'service' => 'melvore-php-api',
    'version' => '2.0.0',
    'storefront' => MelvoreEnv::get('STOREFRONT_BASE_URL', 'https://melvore.co.in'),
    'endpoints' => [
        'health' => '/api/health.php',
        'bootstrap' => '/api/bootstrap.php',
        'home_feed' => '/api/home/feed.php',
        'themes' => '/api/themes/config.php',
        'occasions' => '/api/occasions/active.php',
        'world_map' => '/api/discovery/world_map.php',
        'offers' => '/api/offers/eligible.php',
    ],
], 'Melvore PHP API ready');
