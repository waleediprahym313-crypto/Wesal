-- WESAL public media bucket for profile/post images
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('wesal-media','wesal-media',true,10485760,array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set public=true, file_size_limit=10485760, allowed_mime_types=array['image/jpeg','image/png','image/webp'];
create policy "wesal media public read" on storage.objects for select using (bucket_id='wesal-media');
create policy "wesal media authenticated upload own folder" on storage.objects for insert to authenticated with check (bucket_id='wesal-media' and (storage.foldername(name))[1]=auth.uid()::text);
create policy "wesal media owners delete" on storage.objects for delete to authenticated using (bucket_id='wesal-media' and (storage.foldername(name))[1]=auth.uid()::text);