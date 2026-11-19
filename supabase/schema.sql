-- DeviceXpert Cloud Sync Schema with Row Level Security (RLS)

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    display_name text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.devices (
    id text primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    category text not null check (category in ('ANDROID', 'COMPUTER', 'IOT')),
    connection_status text not null default 'OFFLINE',
    last_seen_at timestamptz not null default now(),
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create table if not exists public.telemetry_logs (
    id bigint generated always as identity primary key,
    device_id text not null references public.devices(id) on delete cascade,
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    recorded_at timestamptz not null default now(),
    health_score smallint not null,
    payload jsonb not null
);

create index if not exists idx_telemetry_logs_device_time
    on public.telemetry_logs (device_id, recorded_at desc);

create table if not exists public.alerts (
    id uuid primary key,
    device_id text not null references public.devices(id) on delete cascade,
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    severity text not null,
    title text not null,
    message text not null,
    is_acknowledged boolean not null default false,
    created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.devices enable row level security;
alter table public.telemetry_logs enable row level security;
alter table public.alerts enable row level security;

create policy "Users manage own profile"
    on public.profiles for all
    using (auth.uid() = id)
    with check (auth.uid() = id);

create policy "Users manage own devices"
    on public.devices for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

create policy "Users manage own telemetry"
    on public.telemetry_logs for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

create policy "Users manage own alerts"
    on public.alerts for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);
