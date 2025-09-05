/**
 * Script para criar buckets necessários no Supabase Storage
 * Execute este arquivo no console do Supabase ou use o SDK admin
 */

-- SQL para criar as policies de storage no Supabase
-- Execute no SQL Editor do Supabase Dashboard

-- Criar policy para chatapp_images
insert into storage.buckets (id, name, public) values ('chatapp_images', 'chatapp_images', true);

-- Criar policy para chatapp_videos  
insert into storage.buckets (id, name, public) values ('chatapp_videos', 'chatapp_videos', true);

-- Criar policy para chatapp_audios
insert into storage.buckets (id, name, public) values ('chatapp_audios', 'chatapp_audios', true);

-- Criar policy para chatapp_files
insert into storage.buckets (id, name, public) values ('chatapp_files', 'chatapp_files', true);

-- Políticas de acesso (permitir inserção, leitura, atualização e exclusão)
-- Para chatapp_images
insert into storage.objects (bucket_id, name, owner, metadata, path_tokens, version)
on conflict do nothing;

create policy "Public Access" on storage.objects for select using (bucket_id = 'chatapp_images');
create policy "Public Upload" on storage.objects for insert with check (bucket_id = 'chatapp_images');
create policy "Public Update" on storage.objects for update using (bucket_id = 'chatapp_images');
create policy "Public Delete" on storage.objects for delete using (bucket_id = 'chatapp_images');

-- Para chatapp_videos
create policy "Public Access" on storage.objects for select using (bucket_id = 'chatapp_videos');
create policy "Public Upload" on storage.objects for insert with check (bucket_id = 'chatapp_videos');
create policy "Public Update" on storage.objects for update using (bucket_id = 'chatapp_videos');
create policy "Public Delete" on storage.objects for delete using (bucket_id = 'chatapp_videos');

-- Para chatapp_audios
create policy "Public Access" on storage.objects for select using (bucket_id = 'chatapp_audios');
create policy "Public Upload" on storage.objects for insert with check (bucket_id = 'chatapp_audios');
create policy "Public Update" on storage.objects for update using (bucket_id = 'chatapp_audios');
create policy "Public Delete" on storage.objects for delete using (bucket_id = 'chatapp_audios');

-- Para chatapp_files
create policy "Public Access" on storage.objects for select using (bucket_id = 'chatapp_files');
create policy "Public Upload" on storage.objects for insert with check (bucket_id = 'chatapp_files');
create policy "Public Update" on storage.objects for update using (bucket_id = 'chatapp_files');
create policy "Public Delete" on storage.objects for delete using (bucket_id = 'chatapp_files');
