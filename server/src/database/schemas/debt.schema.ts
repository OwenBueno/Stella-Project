import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type DebtDocument = HydratedDocument<Debt>;

@Schema({ ...stellaSchemaOptions, collection: 'Debt' })
export class Debt {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ required: true })
  contactName!: string;

  @Prop({ required: true, enum: ['owed_to_me', 'owed_by_me'] })
  direction!: 'owed_to_me' | 'owed_by_me';

  @Prop({ required: true, type: Number })
  totalAmount!: number;

  @Prop({ required: true, type: Number })
  remainingAmount!: number;

  @Prop({ type: Date, default: null })
  dueDate!: Date | null;

  @Prop({ type: String, default: null })
  notes!: string | null;

  @Prop({ required: true, default: false })
  isResolved!: boolean;

  @Prop({ type: Date, required: true })
  createdAt!: Date;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;

  @Prop({ type: Date, default: null })
  deletedAt!: Date | null;
}

export const DebtSchema = SchemaFactory.createForClass(Debt);
DebtSchema.index({ updatedAt: 1 });
DebtSchema.index({ isResolved: 1 });
