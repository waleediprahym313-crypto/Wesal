drop policy if exists "members read messages" on public.messages;
create policy "members read messages" on public.messages for select to authenticated
using (exists (select 1 from public.conversation_members cm where cm.conversation_id=messages.conversation_id and cm.user_id=auth.uid()));
drop policy if exists "members send messages as themselves" on public.messages;
create policy "members send messages as themselves" on public.messages for insert to authenticated
with check (sender_id=auth.uid() and exists (select 1 from public.conversation_members cm where cm.conversation_id=messages.conversation_id and cm.user_id=auth.uid()));
drop policy if exists "users submit own verification request" on public.verification_requests;
create policy "users submit own pending verification request" on public.verification_requests for insert to authenticated
with check (user_id=auth.uid() and status='pending');